package com.example.chat.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.chat.ModelManager;
import com.example.chat.R;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.beans.ModelDto;
import com.example.chat.services.ModelApiService;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelSelectionFragment extends Fragment {
    private static final String TAG = "ModelSelectionFragment";
    private ModelApiService modelApiService;

    private RecyclerView rvModels;
    private SwipeRefreshLayout swipe;
    private EditText searchInput;
    private TextView tvSelectedModel;
    private List<ModelDto> models = new ArrayList<>();
    private ModelAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_model_selection, container, false);

        modelApiService = BackendRetrofitClient.getClient().create(ModelApiService.class);

        rvModels = view.findViewById(R.id.rv_models);
        swipe = view.findViewById(R.id.swipe);
        searchInput = view.findViewById(R.id.search_input);
        tvSelectedModel = view.findViewById(R.id.tv_selected_model);

        rvModels.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ModelAdapter(models);
        rvModels.setAdapter(adapter);

        swipe.setOnRefreshListener(this::fetchModels);

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().toLowerCase();
                adapter.filter(q);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        fetchModels();

        return view;
    }


    private void fetchModels() {
        Log.d(TAG, "fetchModels: 开始获取用户可见模型列表");
        Call<List<ModelDto>> call = modelApiService.listVisibleModels();
        call.enqueue(new Callback<List<ModelDto>>() {
            @Override
            public void onResponse(Call<List<ModelDto>> call, Response<List<ModelDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    models.clear();
                    models.addAll(response.body());
                    adapter.refresh();

                    if (models.isEmpty()) {
                        Toast.makeText(requireContext(), "暂无可用模型", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "fetchModels: 用户可见模型列表为空");
                    }
                    if (!models.isEmpty()) {
                        // 只有在ModelManager中没有选中模型时，才设置默认模型
                        if (ModelManager.getSelectedModelDto() == null) {
                            String firstModel = models.get(0).getModelName();
                            Long firstModelId = models.get(0).getId();
                            ModelManager.setSelectedModel(firstModel);
                            ModelManager.setSelectedModelDto(models.get(0));
                            tvSelectedModel.setText("已选: " + firstModel);
                            Log.d(TAG, "fetchModels: ModelManager为空，设置默认模型，modelID=" + firstModelId + ", 名称=" + firstModel);
                        } else {
                            // 显示当前已选中的模型
                            String selectedModel = ModelManager.getSelectedModelOrDefault();
                            tvSelectedModel.setText("已选: " + selectedModel);
                            Log.d(TAG, "fetchModels: ModelManager已有模型，保持当前选择: " + selectedModel);
                        }
                    }

                    Toast.makeText(requireContext(), "模型加载成功", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "fetchModels: 获取模型失败，code=" + response.code());
                    Toast.makeText(requireContext(), "获取模型失败", Toast.LENGTH_SHORT).show();
                }
                swipe.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<List<ModelDto>> call, Throwable t) {
                Log.e(TAG, "fetchModels: 网络错误: " + t.getMessage());
                Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                swipe.setRefreshing(false);
            }
        });
    }

    private class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.VH> {
        private static final String TAG = "ModelAdapter";
        private final List<ModelDto> items;
        private final List<ModelDto> filtered = new ArrayList<>();

        ModelAdapter(List<ModelDto> items) { this.items = items; this.filtered.addAll(items); }

        void refresh() {
            filtered.clear();
            filtered.addAll(items);
            notifyDataSetChanged();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView name, family, size;
            ImageView avatar;
            VH(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_model_name);
                family = itemView.findViewById(R.id.tv_model_family);
                size = itemView.findViewById(R.id.tv_model_size);
                avatar = itemView.findViewById(R.id.iv_model_avatar);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.model_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ModelDto info = filtered.get(position);
            holder.name.setText(info.getModelName());
            holder.family.setText(info.getModelFamily() != null ? info.getModelFamily() : "");
            holder.size.setText(info.getParameterSize() != null ? info.getParameterSize() : String.valueOf(info.getModelSize()));

            // 加载模型头像
            String avatarUrl = info.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl;
                if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                    fullUrl = "http://10.0.2.2:8080" + (avatarUrl.startsWith("/") ? avatarUrl : "/" + avatarUrl);
                }
                Glide.with(holder.itemView.getContext())
                        .load(fullUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(holder.avatar);
            } else {
                holder.avatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.itemView.setOnClickListener(v -> {
                String sel = info.getModelName();
                Long modelId = info.getId();
                ModelManager.setSelectedModel(sel);
                ModelManager.setSelectedModelDto(info);
                tvSelectedModel.setText("已选: " + sel);
                Log.d(TAG, "onBindViewHolder: 点击模型，modelId=" + modelId + ", modelName=" + sel);
                if (getActivity() != null) {
                    android.content.Intent it = new android.content.Intent(getActivity(), com.example.chat.activities.ModelDetailActivity.class);
                    it.putExtra("model_name", sel);
                    it.putExtra("modelId", modelId);
                    getActivity().startActivity(it);
                }
            });
        }

        @Override
        public int getItemCount() { return filtered.size(); }

        void filter(String q) {
            filtered.clear();
            if (q == null || q.isEmpty()) {
                filtered.addAll(items);
            } else {
                for (ModelDto m : items) {
                    if (m.getModelName() != null && m.getModelName().toLowerCase().contains(q)) {
                        filtered.add(m);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }
}
