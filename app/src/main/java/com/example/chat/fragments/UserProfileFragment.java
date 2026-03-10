package com.example.chat.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.chat.AdminChooseActivity;
import com.example.chat.ChatHistoryActivity;
import com.example.chat.LoginActivity;
import com.example.chat.R;
import com.example.chat.UserProfileDetailActivity;
import com.example.chat.beans.User;
import com.example.chat.managers.UserManager;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.FileUploadService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private ImageView ivUserAvatar;
    private TextView tvUserNickname;
    private TextView tvUserInfo;
    private Button btnChatHistory;
    private Button btnViewProfile;
    private Button btnAdmin;
    private Button btnLogout;
    private Button btnLogin;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        tvUserNickname = view.findViewById(R.id.tv_user_nickname);
        tvUserInfo = view.findViewById(R.id.tv_user_info);
        btnChatHistory = view.findViewById(R.id.btn_chat_history);
        btnViewProfile = view.findViewById(R.id.btn_view_profile);
        btnAdmin = view.findViewById(R.id.btn_admin);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnLogin = view.findViewById(R.id.btn_login);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadAvatar(uri);
            }
        });

        ivUserAvatar.setOnClickListener(v -> showAvatarDialog());

        btnChatHistory.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChatHistoryActivity.class);
            startActivity(intent);
        });

        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UserProfileDetailActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            User user = UserManager.getInstance().getCurrentUser();
            Intent intent = new Intent(requireContext(), AdminChooseActivity.class);
            if (user != null) {
                intent.putExtra("userId", user.getId());
                intent.putExtra("username", user.getUsername());
                intent.putExtra("role", user.getRole());
            }
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());

        btnLogin.setOnClickListener(v -> navigateToLogin());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        User user = UserManager.getInstance().getCurrentUser();

        if (user != null && user.getId() != null && user.getId() != -1) {
            String displayName = user.getNickname() != null && !user.getNickname().isEmpty()
                    ? user.getNickname()
                    : user.getUsername();
            tvUserNickname.setText(displayName);
            tvUserInfo.setText("用户名: " + (user.getUsername() == null ? "" : user.getUsername()));

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Log.d(TAG, "加载头像URL: " + user.getAvatarUrl());
                Glide.with(this)
                        .load(user.getAvatarUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Glide加载头像失败: " + user.getAvatarUrl(), e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d(TAG, "Glide加载头像成功: " + user.getAvatarUrl());
                                return false;
                            }
                        })
                        .into(ivUserAvatar);
            } else {
                ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            btnChatHistory.setVisibility(View.VISIBLE);
            btnViewProfile.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            
            if ("admin".equals(user.getRole())) {
                btnAdmin.setVisibility(View.VISIBLE);
            } else {
                btnAdmin.setVisibility(View.GONE);
            }
        } else {
            tvUserNickname.setText("未登录");
            tvUserInfo.setText("请登录以使用完整功能");
            ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);

            btnChatHistory.setVisibility(View.GONE);
            btnViewProfile.setVisibility(View.GONE);
            btnAdmin.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        }
    }

    private void showAvatarDialog() {
        User user = UserManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_avatar_view, null);
        builder.setView(dialogView);

        ImageView ivAvatarFull = dialogView.findViewById(R.id.iv_avatar_full);
        Button btnChangeAvatar = dialogView.findViewById(R.id.btn_change_avatar);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(user.getAvatarUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivAvatarFull);
        }

        AlertDialog dialog = builder.create();

        btnChangeAvatar.setOnClickListener(v -> {
            dialog.dismiss();
            openImagePicker();
        });

        dialog.show();
    }

    private void openImagePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
            } else {
                pickImageLauncher.launch("image/*");
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            } else {
                pickImageLauncher.launch("image/*");
            }
        }
    }

    private void uploadAvatar(Uri imageUri) {
        Log.d(TAG, "开始上传头像，URI: " + imageUri);
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开输入流");
                Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(requireContext().getCacheDir(), "avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "文件已保存到: " + file.getAbsolutePath() + ", 大小: " + file.length() + " bytes");

            RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            FileUploadService service = BackendRetrofitClient.getClient().create(FileUploadService.class);
            Log.d(TAG, "开始调用上传API");

            User currentUser = UserManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                Log.e(TAG, "用户未登录");
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            service.uploadUserAvatar(body, currentUser.getId()).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    Log.d(TAG, "收到响应: code=" + response.code() + ", isSuccessful=" + response.isSuccessful());
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "响应体: " + response.body().toString());
                        Object successObj = response.body().get("success");
                        boolean success = successObj instanceof Boolean ? (Boolean) successObj : Boolean.parseBoolean(String.valueOf(successObj));
                        if (success) {
                            String avatarUrl = (String) response.body().get("avatarUrl");
                            if (avatarUrl != null) {
                                Log.d(TAG, "头像上传成功，URL: " + avatarUrl);
                                User user = UserManager.getInstance().getCurrentUser();
                                if (user != null) {
                                    user.setAvatarUrl(avatarUrl);
                                    UserManager.getInstance().setCurrentUser(user);
                                }
                                updateUI();
                                Toast.makeText(requireContext(), "头像上传成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "响应中没有avatarUrl字段");
                                Toast.makeText(requireContext(), "上传失败：未获取到头像URL", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String message = (String) response.body().get("message");
                            Log.e(TAG, "上传失败: " + message);
                            Toast.makeText(requireContext(), "上传失败：" + (message != null ? message : "未知错误"), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "上传失败: " + response.code() + " " + response.message());
                        try {
                            if (response.errorBody() != null) {
                                Log.e(TAG, "错误体: " + response.errorBody().string());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "读取错误体失败", e);
                        }
                        Toast.makeText(requireContext(), "上传失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "上传请求失败", t);
                    Toast.makeText(requireContext(), "上传失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "上传过程异常", e);
            Toast.makeText(requireContext(), "上传失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        UserManager.getInstance().logout();
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(requireContext(), "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
