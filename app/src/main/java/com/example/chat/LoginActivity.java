package com.example.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chat.beans.User;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.UserApiService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_PASSWORD = "saved_password";
    private static final String KEY_REMEMBER = "remember_me";

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView registerText;
    private TextView guestLoginText;
    private ProgressBar progressBar;
    private CheckBox cbRemember;
    private ImageView ivUserAvatar;
    private TextView tvUserNickname;

    private UserApiService userApiService;
    private Handler avatarHandler = new Handler(Looper.getMainLooper());
    private Runnable avatarRunnable;
    private static final long AVATAR_DELAY = 500;

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        userApiService = BackendRetrofitClient.getClient().create(UserApiService.class);
        loadSavedCredentials();
        setupClickListeners();
        setupUsernameWatcher();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);
        guestLoginText = findViewById(R.id.guestLoginText);
        progressBar = findViewById(R.id.progressBar);
        cbRemember = findViewById(R.id.cb_remember);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserNickname = findViewById(R.id.tv_user_nickname);
    }

    private void loadSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);
        cbRemember.setChecked(remember);

        if (remember) {
            String savedUsername = prefs.getString(KEY_USERNAME, "");
            String savedPassword = prefs.getString(KEY_PASSWORD, "");
            usernameEditText.setText(savedUsername);
            passwordEditText.setText(savedPassword);

            if (!savedUsername.isEmpty()) {
                fetchUserAvatar(savedUsername);
            }
        }
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        guestLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupUsernameWatcher() {
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String username = s.toString().trim();

                if (avatarRunnable != null) {
                    avatarHandler.removeCallbacks(avatarRunnable);
                }

                if (!username.isEmpty()) {
                    avatarRunnable = () -> fetchUserAvatar(username);
                    avatarHandler.postDelayed(avatarRunnable, AVATAR_DELAY);
                } else {
                    ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                    tvUserNickname.setText("");
                }
            }
        });
    }

    private void fetchUserAvatar(String username) {
        userApiService.getUserByUsername(username).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(LoginActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_gallery)
                                .into(ivUserAvatar);
                    } else {
                        ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                    String nickname = user.getNickname();
                    tvUserNickname.setText(nickname != null && !nickname.isEmpty() ? nickname : "");
                } else {
                    ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                    tvUserNickname.setText("");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                tvUserNickname.setText("");
            }
        });
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEditText.setError("用户名不能为空");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("密码不能为空");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        User user = new User(username, password);

        userApiService.login(user).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    Boolean success = (Boolean) result.get("success");
                    String message = (String) result.get("message");

                    String role = (String) result.get("role");
                    if (role == null) {
                        role = ROLE_USER;
                    }

                    Object userIdObj = result.get("userId");
                    Long userId = null;

                    if (userIdObj != null) {
                        if (userIdObj instanceof Integer) {
                            userId = ((Integer) userIdObj).longValue();
                        } else if (userIdObj instanceof Long) {
                            userId = (Long) userIdObj;
                        } else if (userIdObj instanceof Double) {
                            userId = ((Double) userIdObj).longValue();
                        } else if (userIdObj instanceof String) {
                            try {
                                userId = Long.parseLong((String) userIdObj);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    String responseUsername = (String) result.get("username");
                    if (responseUsername == null) {
                        responseUsername = username;
                    }

                    if (success != null && success) {
                        saveCredentials(username, password, cbRemember.isChecked());
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        fetchFullUserInfoAndNavigate(userId, responseUsername, role);
                    } else {
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCredentials(String username, String password, boolean remember) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (remember) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PASSWORD, password);
        } else {
            editor.clear();
        }

        editor.apply();
    }

    private void fetchFullUserInfoAndNavigate(Long userId, String username, String role) {
        userApiService.getUserByUsername(username).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User fullUser = null;
                if (response.isSuccessful() && response.body() != null) {
                    fullUser = response.body();
                }
                navigateBasedOnRole(userId, username, role, fullUser);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                navigateBasedOnRole(userId, username, role, null);
            }
        });
    }

    private void navigateBasedOnRole(Long userId, String username, String role, User fullUser) {
        Intent intent;

        if (ROLE_ADMIN.equals(role)) {
            intent = new Intent(LoginActivity.this, AdminChooseActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        intent.putExtra("userId", userId);
        intent.putExtra("username", username);
        intent.putExtra("role", role);
        
        if (fullUser != null) {
            fullUser.setRole(role);
            intent.putExtra("nickname", fullUser.getNickname());
            intent.putExtra("avatarUrl", fullUser.getAvatarUrl());
        }

        startActivity(intent);
        finish();
    }
}
