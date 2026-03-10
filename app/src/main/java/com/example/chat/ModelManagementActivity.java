package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import com.example.chat.beans.ModelDto;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.ModelApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelManagementActivity extends AppCompatActivity {
    private Long userId;
    private ModelApiService service;
    private ModelAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_model_management);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent() != null) {
            userId = getIntent().getLongExtra("userId", -1);
        }

        service = BackendRetrofitClient.getClient().create(ModelApiService.class);

        Button btnSync = findViewById(R.id.btn_sync);
        SwipeRefreshLayout swipe = findViewById(R.id.swipe);
        RecyclerView rv = findViewById(R.id.rv_models);
        tvEmpty = findViewById(R.id.tv_empty);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(() -> fetchModels(swipe));

        btnSync.setOnClickListener(v -> {
            btnSync.setEnabled(false);
            Call<Map<String, Object>> call = service.syncModels();
            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    btnSync.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Object msg = response.body().get("message");
                        Toast.makeText(ModelManagementActivity.this, "同步结果: " + (msg != null ? msg.toString() : "完成"), Toast.LENGTH_LONG).show();
                        fetchModels(swipe);
                    } else {
                        Toast.makeText(ModelManagementActivity.this, "同步失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    btnSync.setEnabled(true);
                    Toast.makeText(ModelManagementActivity.this, "同步失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        swipe.setRefreshing(true);
        fetchModels(swipe);
    }

    private void fetchModels(SwipeRefreshLayout swipe) {
        service.listModels().enqueue(new Callback<List<ModelDto>>() {
            @Override
            public void onResponse(Call<List<ModelDto>> call, Response<List<ModelDto>> response) {
                if (swipe != null) swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateItems(response.body());
                    updateEmptyView();
                } else {
                    Toast.makeText(ModelManagementActivity.this, "获取模型列表失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ModelDto>> call, Throwable t) {
                if (swipe != null) swipe.setRefreshing(false);
                Toast.makeText(ModelManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void toggleModelDisplay(ModelDto model, int position) {
        boolean newDisplay = model.getIsDisplay() == null || !model.getIsDisplay();

        Map<String, Object> body = new HashMap<>();
        body.put("isDisplay", newDisplay);

        service.adminToggleModelDisplay(model.getId(), body, userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object ok = response.body().get("success");
                    boolean success = ok instanceof Boolean ? (Boolean) ok : Boolean.parseBoolean(String.valueOf(ok));
                    if (success) {
                        model.setIsDisplay(newDisplay);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(ModelManagementActivity.this, newDisplay ? "已显示给用户" : "已对用户隐藏", Toast.LENGTH_SHORT).show();
                    } else {
                        String msg = (String) response.body().get("message");
                        Toast.makeText(ModelManagementActivity.this, msg != null ? msg : "操作失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ModelManagementActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ModelManagementActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.VH> {
        private List<ModelDto> items;

        ModelAdapter(List<ModelDto> items) {
            this.items = items;
        }

        void updateItems(List<ModelDto> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_manage, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ModelDto m = items.get(position);
            holder.title.setText(m.getModelName());
            String subtitle = (m.getModelFamily() != null ? m.getModelFamily() + " • " : "") + (m.getParameterSize() != null ? m.getParameterSize() : "");
            holder.subtitle.setText(subtitle);

            boolean isDisplay = m.getIsDisplay() == null || m.getIsDisplay();
            holder.tvDisplayStatus.setText(isDisplay ? "已显示" : "已隐藏");
            holder.tvDisplayStatus.setTextColor(isDisplay ? 0xFF4CAF50 : 0xFF9E9E9E);
            holder.tvDisplayStatus.setBackgroundColor(isDisplay ? 0xFFE8F5E9 : 0xFFE0E0E0);

            holder.btnToggleDisplay.setText(isDisplay ? "隐藏" : "显示");
            holder.btnToggleDisplay.setBackgroundColor(isDisplay ? 0xFFF44336 : 0xFF4CAF50);

            holder.btnToggleDisplay.setOnClickListener(v -> toggleModelDisplay(m, position));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ModelManagementActivity.this, AdminModelDetailActivity.class);
                intent.putExtra("modelId", m.getId());
                if (userId != null && userId > 0) {
                    intent.putExtra("userId", userId);
                }
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;
            TextView tvDisplayStatus;
            Button btnToggleDisplay;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_model_name);
                subtitle = itemView.findViewById(R.id.tv_model_sub);
                tvDisplayStatus = itemView.findViewById(R.id.tv_display_status);
                btnToggleDisplay = itemView.findViewById(R.id.btn_toggle_display);
            }
        }
    }
}
