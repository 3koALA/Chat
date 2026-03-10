package com.example.chat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.chat.beans.User;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.UserAdminService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManagementActivity extends AppCompatActivity {

    private UserAdminService userService;
    private UserAdapter adapter;
    private SwipeRefreshLayout swipe;
    private EditText etSearch;
    private TextView tvEmpty;
    private List<User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        swipe = findViewById(R.id.swipe);
        RecyclerView rv = findViewById(R.id.rv_users);
        View btnAdd = findViewById(R.id.btn_add_user);
        etSearch = findViewById(R.id.et_search);
        View btnSearch = findViewById(R.id.btn_search);
        tvEmpty = findViewById(R.id.tv_empty);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        this.userService = BackendRetrofitClient.getClient().create(UserAdminService.class);

        swipe.setOnRefreshListener(() -> fetchUsers());

        btnAdd.setOnClickListener(v -> showAddUserDialog());

        btnSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim().toLowerCase();
            filterUsers(keyword);
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = etSearch.getText().toString().trim().toLowerCase();
            filterUsers(keyword);
            return true;
        });

        adapter.setOnItemClickListener(user -> {
            android.content.Intent it = new android.content.Intent(UserManagementActivity.this, UserDetailActivity.class);
            it.putExtra("userId", user.getId());
            startActivity(it);
        });

        adapter.setOnItemLongClickListener(user -> showDeleteConfirm(user));

        swipe.setRefreshing(true);
        fetchUsers();
    }

    private void fetchUsers() {
        Call<List<User>> call = userService.listUsers();
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allUsers = response.body();
                    String keyword = etSearch.getText().toString().trim().toLowerCase();
                    if (keyword.isEmpty()) {
                        adapter.updateItems(allUsers);
                    } else {
                        filterUsers(keyword);
                    }
                    updateEmptyView();
                } else {
                    Toast.makeText(UserManagementActivity.this, "获取用户列表失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                swipe.setRefreshing(false);
                Toast.makeText(UserManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String keyword) {
        if (keyword.isEmpty()) {
            adapter.updateItems(allUsers);
        } else {
            List<User> filtered = new ArrayList<>();
            for (User u : allUsers) {
                if ((u.getUsername() != null && u.getUsername().toLowerCase().contains(keyword)) ||
                    (u.getNickname() != null && u.getNickname().toLowerCase().contains(keyword))) {
                    filtered.add(u);
                }
            }
            adapter.updateItems(filtered);
        }
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showAddUserDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        final EditText etUsername = new EditText(this);
        etUsername.setHint("用户名");
        layout.addView(etUsername);
        final EditText etNickname = new EditText(this);
        etNickname.setHint("昵称 (可选)");
        layout.addView(etNickname);
        final EditText etPassword = new EditText(this);
        etPassword.setHint("密码");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        new AlertDialog.Builder(this)
                .setTitle("添加用户")
                .setView(layout)
                .setPositiveButton("创建", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String nickname = etNickname.getText().toString().trim();
                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "用户名和密码为必填项", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    User u = new User();
                    u.setUsername(username);
                    u.setPassword(password);
                    u.setNickname(nickname);
                    Call<java.util.Map<String, Object>> call = userService.createUser(u);
                    call.enqueue(new Callback<java.util.Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Object ok = response.body().get("success");
                                boolean success = ok instanceof Boolean ? (Boolean) ok : Boolean.parseBoolean(String.valueOf(ok));
                                Toast.makeText(UserManagementActivity.this, success ? "创建成功" : "创建失败", Toast.LENGTH_SHORT).show();
                                if (success) fetchUsers();
                            } else {
                                Toast.makeText(UserManagementActivity.this, "创建失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                            Toast.makeText(UserManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteConfirm(User user) {
        new AlertDialog.Builder(this)
                .setTitle("删除用户")
                .setMessage("确定删除用户 " + user.getUsername() + " ?")
                .setPositiveButton("删除", (dialog, which) -> {
                    Call<java.util.Map<String, Object>> call = userService.deleteUser(user.getId());
                    call.enqueue(new Callback<java.util.Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<java.util.Map<String, Object>> call, Response<java.util.Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Object ok = response.body().get("success");
                                boolean success = ok instanceof Boolean ? (Boolean) ok : Boolean.parseBoolean(String.valueOf(ok));
                                Toast.makeText(UserManagementActivity.this, success ? "删除成功" : "删除失败", Toast.LENGTH_SHORT).show();
                                if (success) fetchUsers();
                            } else {
                                Toast.makeText(UserManagementActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                            Toast.makeText(UserManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private List<User> items;
        private OnItemClickListener onItemClickListener;
        private OnItemLongClickListener onItemLongClickListener;

        interface OnItemClickListener {
            void onClick(User user);
        }

        interface OnItemLongClickListener {
            void onLongClick(User user);
        }

        void setOnItemClickListener(OnItemClickListener l) { this.onItemClickListener = l; }
        void setOnItemLongClickListener(OnItemLongClickListener l) { this.onItemLongClickListener = l; }

        UserAdapter(List<User> items) { this.items = items; }

        void updateItems(List<User> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            User u = items.get(position);
            holder.tvUsername.setText(u.getUsername());
            holder.tvNickname.setText(u.getNickname() != null && !u.getNickname().isEmpty() ? u.getNickname() : "未设置昵称");
            holder.tvRole.setText(u.getRole() != null ? u.getRole() : "user");
            
            if ("admin".equals(u.getRole())) {
                holder.tvRole.setBackgroundColor(0xFF4CAF50);
            } else {
                holder.tvRole.setBackgroundColor(0xFF9E9E9E);
            }

            if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(u.getAvatarUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) onItemClickListener.onClick(u);
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onLongClick(u);
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvUsername;
            TextView tvNickname;
            TextView tvRole;
            VH(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvNickname = itemView.findViewById(R.id.tv_nickname);
                tvRole = itemView.findViewById(R.id.tv_role);
            }
        }
    }
}
