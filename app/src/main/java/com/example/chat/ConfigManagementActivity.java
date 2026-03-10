package com.example.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.beans.ModelsConfig;
import com.example.chat.beans.User;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.ModelApiService;
import com.example.chat.services.UserAdminService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfigManagementActivity extends AppCompatActivity {
    private UserAdminService userService;
    private ModelApiService modelService;

    private RecyclerView rvUsers;
    private RecyclerView rvConfigs;
    private ProgressBar progressUsers;
    private ProgressBar progressConfigs;

    private UserAdapter userAdapter;
    private ConfigAdapter configAdapter;

    private long selectedUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_management);

        userService = BackendRetrofitClient.getClient().create(UserAdminService.class);
        modelService = BackendRetrofitClient.getClient().create(ModelApiService.class);

        rvUsers = findViewById(R.id.rv_users);
        rvConfigs = findViewById(R.id.rv_configs);
        progressUsers = findViewById(R.id.progress_users);
        progressConfigs = findViewById(R.id.progress_configs);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvConfigs.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new ArrayList<>());
        configAdapter = new ConfigAdapter(new ArrayList<>());
        rvUsers.setAdapter(userAdapter);
        rvConfigs.setAdapter(configAdapter);

        userAdapter.setOnClickListener(u -> {
            selectedUserId = u.getId();
            loadConfigsForUser(selectedUserId);
        });

        configAdapter.setOnItemClickListener(cfg -> {
            // show details in a simple details activity/dialog
            showConfigDetail(cfg);
        });

        loadUsers();
    }

    private void loadUsers() {
        progressUsers.setVisibility(View.VISIBLE);
        userService.listUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                progressUsers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    userAdapter.updateItems(response.body());
                } else {
                    Toast.makeText(ConfigManagementActivity.this, "加载用户失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                progressUsers.setVisibility(View.GONE);
                Toast.makeText(ConfigManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadConfigsForUser(long userId) {
        progressConfigs.setVisibility(View.VISIBLE);
        Long header = userId > 0 ? userId : null;
        modelService.listConfigsByUser(userId, header).enqueue(new Callback<List<ModelsConfig>>() {
            @Override
            public void onResponse(Call<List<ModelsConfig>> call, Response<List<ModelsConfig>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    progressConfigs.setVisibility(View.GONE);
                    configAdapter.updateItems(response.body());
                } else {
                    // 如果后端没有按用户列出配置的接口（API_DOC.md 中未声明），尝试回退使用 admin-configs 查询（按 adminId）
                    attemptFallbackLoadByAdmin(userId);
                }
            }

            @Override
            public void onFailure(Call<List<ModelsConfig>> call, Throwable t) {
                // 回退尝试
                attemptFallbackLoadByAdmin(userId);
            }
        });
    }

    private void attemptFallbackLoadByAdmin(long userId) {
        // 使用当前登录用户 id 作为 adminId 查询 /api/admin-configs/admin/{adminId}，再根据 configId 获取配置详情
        com.example.chat.beans.User cur = com.example.chat.managers.UserManager.getInstance().getCurrentUser();
        if (cur == null) {
            progressConfigs.setVisibility(View.GONE);
            Toast.makeText(this, "无法获取当前用户，无法回退查询。", Toast.LENGTH_SHORT).show();
            return;
        }
        long adminId = cur.getId();
        modelService.listAdminConfigsByAdmin(adminId).enqueue(new Callback<List<java.util.Map<String,Object>>>() {
            @Override
            public void onResponse(Call<List<java.util.Map<String,Object>>> call, Response<List<java.util.Map<String,Object>>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    progressConfigs.setVisibility(View.GONE);
                    Toast.makeText(ConfigManagementActivity.this, "后端未提供按用户列出配置接口，回退查询也失败。", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<java.util.Map<String,Object>> list = response.body();
                if (list.isEmpty()) {
                    progressConfigs.setVisibility(View.GONE);
                    configAdapter.updateItems(new ArrayList<>());
                    return;
                }
                // 收集 configId 并逐个获取详情
                List<ModelsConfig> configs = new ArrayList<>();
                java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(list.size());
                for (java.util.Map<String,Object> entry : list) {
                    Object cidObj = entry.get("configId");
                    if (cidObj == null) {
                        if (remaining.decrementAndGet() == 0) {
                            progressConfigs.setVisibility(View.GONE);
                            configAdapter.updateItems(configs);
                        }
                        continue;
                    }
                    long cfgId = ((Number)cidObj).longValue();
                    modelService.getConfigById(cfgId).enqueue(new Callback<ModelsConfig>() {
                        @Override
                        public void onResponse(Call<ModelsConfig> call, Response<ModelsConfig> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                configs.add(response.body());
                            }
                            if (remaining.decrementAndGet() == 0) {
                                progressConfigs.setVisibility(View.GONE);
                                configAdapter.updateItems(configs);
                            }
                        }

                        @Override
                        public void onFailure(Call<ModelsConfig> call, Throwable t) {
                            if (remaining.decrementAndGet() == 0) {
                                progressConfigs.setVisibility(View.GONE);
                                configAdapter.updateItems(configs);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<java.util.Map<String,Object>>> call, Throwable t) {
                progressConfigs.setVisibility(View.GONE);
                Toast.makeText(ConfigManagementActivity.this, "回退查询失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfigDetail(ModelsConfig cfg) {
        // show simple details and delete button
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_config_detail, null);
        TextView tv = v.findViewById(R.id.tv_config_detail);
        Button btnDelete = v.findViewById(R.id.btn_delete_config);
        tv.setText(buildCfgText(cfg));
        b.setView(v);
        android.app.AlertDialog dlg = b.create();
        btnDelete.setOnClickListener(view -> {
            // delete via API
            Long adminId = null;
            com.example.chat.beans.User cur = com.example.chat.managers.UserManager.getInstance().getCurrentUser();
            if (cur != null) adminId = cur.getId();
            modelService.deleteConfigById(cfg.getId(), adminId).enqueue(new Callback<java.util.Map<String,Object>>() {
                @Override
                public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Object ok = response.body().get("success");
                        boolean success = ok instanceof Boolean ? (Boolean) ok : Boolean.parseBoolean(String.valueOf(ok));
                        if (success) {
                            Toast.makeText(ConfigManagementActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            dlg.dismiss();
                            if (selectedUserId > 0) loadConfigsForUser(selectedUserId);
                        } else {
                            Toast.makeText(ConfigManagementActivity.this, "删除失败或权限不足", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ConfigManagementActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) {
                    Toast.makeText(ConfigManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        dlg.show();
    }

    private String buildCfgText(ModelsConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("configId: ").append(c.getId()).append('\n');
        sb.append("modelId: ").append(c.getModelId()).append('\n');
        sb.append("userId: ").append(c.getUserId()).append('\n');
        sb.append("temperature: ").append(c.getTemperature()).append('\n');
        sb.append("topP: ").append(c.getTopP()).append('\n');
        sb.append("topK: ").append(c.getTopK()).append('\n');
        sb.append("numCtx: ").append(c.getNumCtx()).append('\n');
        sb.append("maxTokens: ").append(c.getMaxTokens()).append('\n');
        sb.append("prompt:\n").append(c.getPrompt());
        return sb.toString();
    }

    // --- adapters ---
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private List<User> items;
        private OnClickListener listener;

        interface OnClickListener { void onClick(User u); }

        void setOnClickListener(OnClickListener l) { this.listener = l; }

        UserAdapter(List<User> items) { this.items = items; }

        void updateItems(List<User> list) { this.items = list; notifyDataSetChanged(); }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            User u = items.get(position);
            holder.title.setText(u.getUsername());
            holder.subtitle.setText(u.getNickname() != null ? u.getNickname() : (u.getRole() != null ? u.getRole() : ""));
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(u); });
        }

        @Override public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            VH(View it) { super(it); title = it.findViewById(android.R.id.text1); subtitle = it.findViewById(android.R.id.text2); }
        }
    }

    private static class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.VH> {
        private List<ModelsConfig> items;
        private OnItemClickListener listener;

        interface OnItemClickListener { void onClick(ModelsConfig c); }

        void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

        ConfigAdapter(List<ModelsConfig> items) { this.items = items; }

        void updateItems(List<ModelsConfig> list) { this.items = list; notifyDataSetChanged(); }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ModelsConfig c = items.get(position);
            holder.title.setText("Config#" + c.getId());
            holder.subtitle.setText("temp:" + c.getTemperature() + " topP:" + c.getTopP());
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(c); });
        }

        @Override public int getItemCount() { return items == null ? 0 : items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            VH(View it) { super(it); title = it.findViewById(android.R.id.text1); subtitle = it.findViewById(android.R.id.text2); }
        }
    }
}
