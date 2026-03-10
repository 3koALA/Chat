package com.example.chat.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.MainActivity;
import com.example.chat.ModelManager;
import com.example.chat.beans.OllamaChatRequest;
import com.example.chat.beans.OllamaMessage;
import com.example.chat.services.OllamaApiService;
import com.example.chat.R;
import com.example.chat.retrofitclient.ChatRetrofitClient;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.ChatApiService;
import com.example.chat.beans.Conversation;
import com.example.chat.beans.Message;
import com.google.gson.Gson;

import org.json.JSONObject;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import com.example.chat.services.ModelApiService;
import com.example.chat.beans.ModelDto;
import com.example.chat.beans.ModelsConfig;
import com.example.chat.beans.ModelWithEffectiveResponse;

public class ChatFragment extends Fragment {

    private EditText messageInput;
    private Button sendButton;
    private TextView thinkDisplay;
    private Button toggleThinkButton;
    private ScrollView thinkScrollView;
    private LinearLayout thinkContainer;
    private ImageButton historyButton;
    private ImageButton newChatButton;
    private Button chooseConfigButton;
    private TextView selectedConfigText;
    private TextView conversationTitle;
    private TextView tvChatModel;
    private DrawerLayout drawerLayout;
    private RecyclerView conversationList;
    private RecyclerView rvChatMessages;
    private Button sidebarNewChatButton;
    private ImageButton closeHistoryButton;

    private Long currentConversationId = null;
    private Long userId;
    private String username;

    private ChatApiService chatApiService;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversations = new ArrayList<>();
    private final List<Message> currentConversationMessages = new ArrayList<>();
    private ChatMessageAdapter chatMessageAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // 初始化UI组件
        initViews(view);

        // 初始化后端API服务
        Retrofit backendRetrofit = BackendRetrofitClient.getClient();
        chatApiService = backendRetrofit.create(ChatApiService.class);

        // 获取用户信息
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            userId = activity.getUserId();
            username = activity.getUsername();

            // 初始化对话列表
            initConversationList();

            // 初始化聊天消息列表
            initChatMessageList();

            // 创建或获取当前会话
            createOrGetConversation();
        }

        // 设置监听器
        setupListeners();

        // 配置选择按钮
        chooseConfigButton.setOnClickListener(v -> showConfigSelection());

        // 显示当前模型名（与发送请求时使用的模型一致）
        refreshChatModelDisplay();
        // 显示当前选中的配置
        refreshSelectedConfigDisplay();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到聊天页时刷新模型名，保证与请求使用的模型一致
        refreshChatModelDisplay();
        // 刷新配置显示
        refreshSelectedConfigDisplay();
    }

    /** 刷新界面显示的当前模型名，与 ModelManager 中选中的模型保持一致 */
    private void refreshChatModelDisplay() {
        if (tvChatModel != null) {
            tvChatModel.setText("模型: " + ModelManager.getSelectedModelOrDefault());
        }
    }

    /** 刷新界面显示的当前配置 */
    private void refreshSelectedConfigDisplay() {
        if (selectedConfigText != null) {
            long configId = ModelManager.getSelectedConfigId();
            if (configId > 0) {
                // 调用 API 获取配置详情以显示配置名
                ModelApiService api = BackendRetrofitClient.getClient().create(ModelApiService.class);
                api.getConfigById(configId).enqueue(new Callback<ModelsConfig>() {
                    @Override
                    public void onResponse(Call<ModelsConfig> call, Response<ModelsConfig> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ModelsConfig config = response.body();
                            String displayName = config.getName();
                            if (displayName == null || displayName.isEmpty()) {
                                displayName = "Config#" + configId;
                            }
                            selectedConfigText.setText(displayName);
                        } else {
                            selectedConfigText.setText("Config#" + configId);
                        }
                    }

                    @Override
                    public void onFailure(Call<ModelsConfig> call, Throwable t) {
                        selectedConfigText.setText("Config#" + configId);
                    }
                });
            } else {
                selectedConfigText.setText("默认配置");
            }
        }
    }

    private void initViews(View view) {
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        chooseConfigButton = view.findViewById(R.id.btn_choose_config);
        selectedConfigText = view.findViewById(R.id.tv_selected_config);
        thinkDisplay = view.findViewById(R.id.think_display);
        toggleThinkButton = view.findViewById(R.id.toggle_think_button);
        thinkScrollView = view.findViewById(R.id.think_scroll_view);
        thinkContainer = view.findViewById(R.id.think_container);
        historyButton = view.findViewById(R.id.history_button);
        newChatButton = view.findViewById(R.id.new_chat_button);
        conversationTitle = view.findViewById(R.id.conversation_title);
        tvChatModel = view.findViewById(R.id.tv_chat_model);
        drawerLayout = view.findViewById(R.id.drawer_layout);
        conversationList = view.findViewById(R.id.conversation_list);
        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        sidebarNewChatButton = view.findViewById(R.id.sidebar_new_chat_button);
        closeHistoryButton = view.findViewById(R.id.close_history_button);
    }

    private void initChatMessageList() {
        rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        chatMessageAdapter = new ChatMessageAdapter(currentConversationMessages);
        rvChatMessages.setAdapter(chatMessageAdapter);
    }

    private void initConversationList() {
        conversationList.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationAdapter = new ConversationAdapter(conversations, new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                // 切换对话
                switchConversation(conversation);
                // 关闭抽屉
                drawerLayout.closeDrawers();
            }

            @Override
            public void onDeleteConversation(Conversation conversation) {
                // 显示确认删除对话框
                showDeleteConfirmationDialog(conversation);
            }
        });
        conversationList.setAdapter(conversationAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                messageInput.setText("");
                
                // 立即在UI上显示用户消息
                appendMessage("用户", message);

                if (currentConversationId == null) {
                    // 用户首次发消息，创建新会话，标题用消息内容
                    Conversation newConversation = new Conversation(userId, message);
                    chatApiService.createConversation(newConversation).enqueue(new Callback<Conversation>() {
                        @Override
                        public void onResponse(Call<Conversation> call, Response<Conversation> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                currentConversationId = response.body().getId();
                                conversationTitle.setText(response.body().getTitle());
                                conversations.add(0, response.body());
                                conversationAdapter.notifyItemInserted(0);

                                // 保存用户消息到后端（不重复添加到UI）
                                saveMessageToBackend(message, true, false);

                                // 发送到Ollama，先获取当前选中的 config（如果有）
                                maybeFetchConfigAndSend(message);
                            } else {
                                Toast.makeText(getContext(), "创建对话失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Conversation> call, Throwable t) {
                            Log.e("ChatFragment", "创建会话失败: " + t.getMessage());
                            Toast.makeText(getContext(), "创建对话失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 已有会话，直接保存消息和发送（不重复添加到UI）
                    saveMessageToBackend(message, true, false);
                    maybeFetchConfigAndSend(message);
                }
            }
        });

        toggleThinkButton.setOnClickListener(v -> {
            if (thinkScrollView.getVisibility() == View.VISIBLE) {
                thinkScrollView.setVisibility(View.GONE);
                toggleThinkButton.setText("显示思考");
                thinkContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                thinkScrollView.setVisibility(View.VISIBLE);
                toggleThinkButton.setText("隐藏思考");
                thinkContainer.getLayoutParams().height = (int) (180 * getResources().getDisplayMetrics().density);
            }
        });

        historyButton.setOnClickListener(v -> {
            // 打开对话历史侧边栏
            drawerLayout.openDrawer(GravityCompat.START);
            // 刷新对话列表
            loadConversations();
        });

        newChatButton.setOnClickListener(v -> showCreateConversationDialog());

        sidebarNewChatButton.setOnClickListener(v -> {
            showCreateConversationDialog();
            drawerLayout.closeDrawers();
        });

        closeHistoryButton.setOnClickListener(v -> drawerLayout.closeDrawers());
    }

    private void showConfigSelection() {
        // 获取当前用户的配置列表
        MainActivity ma = (MainActivity) getActivity();
        if (ma == null) return;
        long uid = ma.getUserId();

        if (uid <= 0) {
            Toast.makeText(getContext(), "请先登录以选择配置", Toast.LENGTH_SHORT).show();
            return;
        }
        ModelApiService api = BackendRetrofitClient.getClient().create(ModelApiService.class);
        Long header = uid > 0 ? uid : null;

        // 获取当前选中的模型
        ModelDto selectedModel = ModelManager.getSelectedModelDto();
        long modelId = selectedModel != null ? selectedModel.getId() : -1;

        Log.d("ChatFragment", "Fetching configs for user: " + uid + ", modelId: " + modelId);

        // 同时获取用户配置和模板
        api.listConfigsByUser(uid, header).enqueue(new Callback<java.util.List<ModelsConfig>>() {
            @Override
            public void onResponse(Call<java.util.List<ModelsConfig>> call, Response<java.util.List<ModelsConfig>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 过滤出当前模型的配置
                    java.util.List<ModelsConfig> userConfigs = new ArrayList<>();
                    for (ModelsConfig config : response.body()) {
                        // 如果指定了模型ID，只显示该模型的配置；否则显示所有配置
                        if (modelId <= 0 || (config.getModelId() != null && config.getModelId() == modelId)) {
                            userConfigs.add(config);
                        }
                    }

                    // 获取模板
                    api.listPublicTemplates().enqueue(new Callback<java.util.List<java.util.Map<String, Object>>>() {
                        @Override
                        public void onResponse(Call<java.util.List<java.util.Map<String, Object>>> call, Response<java.util.List<java.util.Map<String, Object>>> response) {
                            java.util.List<java.util.Map<String, Object>> templates = new ArrayList<>();
                            if (response.isSuccessful() && response.body() != null) {
                                // 过滤出有效的模板
                                for (java.util.Map<String, Object> template : response.body()) {
                                    Object configIdObj = template.get("configId");
                                    if (configIdObj == null) configIdObj = template.get("configid");
                                    if (configIdObj == null) configIdObj = template.get("config_id");
                                    
                                    Object nameObj = template.get("name");
                                    if (nameObj == null) nameObj = template.get("Name");
                                    
                                    if (configIdObj != null && nameObj != null && !nameObj.toString().isEmpty()) {
                                        templates.add(template);
                                    }
                                }
                            }

                            // 合并用户配置和模板
                            int totalItems = 1 + userConfigs.size() + templates.size();
                            String[] names = new String[totalItems];
                            names[0] = "默认配置 (仅模型名和聊天内容)";

                            // 添加用户配置
                            for (int i = 0; i < userConfigs.size(); i++) {
                                ModelsConfig c = userConfigs.get(i);
                                String configName = c.getName();
                                if (configName == null || configName.isEmpty()) {
                                    configName = "配置 #" + c.getId();
                                }
                                names[i + 1] = "我的配置: " + configName + " — temp:" + c.getTemperature() + " topP:" + c.getTopP();
                            }

                            // 添加模板
                            int userConfigCount = userConfigs.size();
                            for (int i = 0; i < templates.size(); i++) {
                                java.util.Map<String, Object> template = templates.get(i);
                                Object nameObj = template.get("name");
                                if (nameObj == null) nameObj = template.get("Name");
                                names[userConfigCount + i + 1] = "模板: " + nameObj.toString();
                            }

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("选择配置")
                                    .setItems(names, (dialog, which) -> {
                                        if (which == 0) {
                                            // 选择默认配置
                                            ModelManager.setSelectedConfigId(-1);
                                            selectedConfigText.setText("默认配置");
                                            refreshChatModelDisplay();
                                        } else if (which <= userConfigCount) {
                                            // 选择用户配置
                                            ModelsConfig sel = userConfigs.get(which - 1);
                                            ModelManager.setSelectedConfigId(sel.getId());
                                            String displayName = sel.getName();
                                            if (displayName == null || displayName.isEmpty()) {
                                                displayName = "Config#" + sel.getId();
                                            }
                                            selectedConfigText.setText(displayName);
                                            // 同步为当前配置对应的模型
                                            api.getModelById(sel.getModelId(), header).enqueue(new Callback<ModelWithEffectiveResponse>() {
                                                @Override
                                                public void onResponse(Call<ModelWithEffectiveResponse> call, Response<ModelWithEffectiveResponse> response) {
                                                    if (response.isSuccessful() && response.body() != null && response.body().getModel() != null) {
                                                        ModelDto dto = response.body().getModel();
                                                        ModelManager.setSelectedModelDto(dto);
                                                        ModelManager.setSelectedModel(dto.getModelName());
                                                        refreshChatModelDisplay();
                                                    }
                                                }
                                                @Override
                                                public void onFailure(Call<ModelWithEffectiveResponse> call, Throwable t) { }
                                            });
                                        } else {
                                            // 选择模板
                                            java.util.Map<String, Object> template = templates.get(which - userConfigCount - 1);
                                            Object configIdObj = template.get("configId");
                                            if (configIdObj == null) configIdObj = template.get("configid");
                                            if (configIdObj == null) configIdObj = template.get("config_id");
                                            
                                            if (configIdObj != null) {
                                                Long configId;
                                                try {
                                                    if (configIdObj instanceof Double) {
                                                        configId = ((Double) configIdObj).longValue();
                                                    } else {
                                                        configId = Long.valueOf(configIdObj.toString());
                                                    }
                                                    // 加载模板配置
                                                    api.getConfigById(configId).enqueue(new Callback<ModelsConfig>() {
                                                        @Override
                                                        public void onResponse(Call<ModelsConfig> call, Response<ModelsConfig> response) {
                                                            if (response.isSuccessful() && response.body() != null) {
                                                                ModelsConfig config = response.body();
                                                                ModelManager.setSelectedConfigId(config.getId());
                                                                String displayName = config.getName();
                                                                if (displayName == null || displayName.isEmpty()) {
                                                                    displayName = "模板#" + config.getId();
                                                                }
                                                                selectedConfigText.setText(displayName);
                                                                // 同步为当前配置对应的模型
                                                                api.getModelById(config.getModelId(), header).enqueue(new Callback<ModelWithEffectiveResponse>() {
                                                                    @Override
                                                                    public void onResponse(Call<ModelWithEffectiveResponse> call, Response<ModelWithEffectiveResponse> response) {
                                                                        if (response.isSuccessful() && response.body() != null && response.body().getModel() != null) {
                                                                            ModelDto dto = response.body().getModel();
                                                                            ModelManager.setSelectedModelDto(dto);
                                                                            ModelManager.setSelectedModel(dto.getModelName());
                                                                            refreshChatModelDisplay();
                                                                        }
                                                                    }
                                                                    @Override
                                                                    public void onFailure(Call<ModelWithEffectiveResponse> call, Throwable t) { }
                                                                });
                                                            }
                                                        }
                                                        @Override
                                                        public void onFailure(Call<ModelsConfig> call, Throwable t) {
                                                            Toast.makeText(getContext(), "加载模板失败", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                } catch (NumberFormatException e) {
                                                    Toast.makeText(getContext(), "模板配置无效", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }

                        @Override
                        public void onFailure(Call<java.util.List<java.util.Map<String, Object>>> call, Throwable t) {
                            // 如果获取模板失败，只显示用户配置
                            int totalItems = 1 + userConfigs.size();
                            String[] names = new String[totalItems];
                            names[0] = "默认配置 (仅模型名和聊天内容)";
                            for (int i = 0; i < userConfigs.size(); i++) {
                                ModelsConfig c = userConfigs.get(i);
                                String configName = c.getName();
                                if (configName == null || configName.isEmpty()) {
                                    configName = "配置 #" + c.getId();
                                }
                                names[i + 1] = configName + " — temp:" + c.getTemperature() + " topP:" + c.getTopP();
                            }
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("选择配置")
                                    .setItems(names, (dialog, which) -> {
                                        if (which == 0) {
                                            ModelManager.setSelectedConfigId(-1);
                                            selectedConfigText.setText("默认配置");
                                            refreshChatModelDisplay();
                                        } else {
                                            ModelsConfig sel = userConfigs.get(which - 1);
                                            ModelManager.setSelectedConfigId(sel.getId());
                                            String displayName = sel.getName();
                                            if (displayName == null || displayName.isEmpty()) {
                                                displayName = "Config#" + sel.getId();
                                            }
                                            selectedConfigText.setText(displayName);
                                            api.getModelById(sel.getModelId(), header).enqueue(new Callback<ModelWithEffectiveResponse>() {
                                                @Override
                                                public void onResponse(Call<ModelWithEffectiveResponse> call, Response<ModelWithEffectiveResponse> response) {
                                                    if (response.isSuccessful() && response.body() != null && response.body().getModel() != null) {
                                                        ModelDto dto = response.body().getModel();
                                                        ModelManager.setSelectedModelDto(dto);
                                                        ModelManager.setSelectedModel(dto.getModelName());
                                                        refreshChatModelDisplay();
                                                    }
                                                }
                                                @Override
                                                public void onFailure(Call<ModelWithEffectiveResponse> call, Throwable t) { }
                                            });
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "获取配置失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<ModelsConfig>> call, Throwable t) {
                Toast.makeText(getContext(), "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 获取用户所有配置
     */
    private void fetchAllUserConfigs(long uid, Long header, ModelApiService api) {
        api.listConfigsByUser(uid, header).enqueue(new Callback<java.util.List<ModelsConfig>>() {
            @Override
            public void onResponse(Call<java.util.List<ModelsConfig>> call, Response<java.util.List<ModelsConfig>> response) {
                Log.d("ChatFragment", "Response code: " + response.code());
                Log.d("ChatFragment", "Response success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {

                    java.util.List<ModelsConfig> list = response.body();

                    Log.d("ChatFragment", "Config list size: " + list.size());

                    // 打印每个配置的详细信息
                    for (int i = 0; i < list.size(); i++) {
                        ModelsConfig config = list.get(i);
                        Log.d("ChatFragment", "Config " + i + ": id=" + config.getId() +
                                ", temperature=" + config.getTemperature() +
                                ", topP=" + config.getTopP());
                    }
                    
                    // 添加默认配置选项
                    String[] names = new String[list.size() + 1];
                    names[0] = "默认配置 (仅模型名和聊天内容)";
                    for (int i = 0; i < list.size(); i++) {
                        ModelsConfig c = list.get(i);
                        names[i + 1] = "#" + c.getId() + " — temp:" + c.getTemperature() + " topP:" + c.getTopP();
                    }
                    new AlertDialog.Builder(requireContext())
                            .setTitle("选择配置")
                            .setItems(names, (dialog, which) -> {
                                if (which == 0) {
                                    // 选择默认配置
                                    ModelManager.setSelectedConfigId(-1);
                                    selectedConfigText.setText("默认配置");
                                    refreshChatModelDisplay();
                                } else {
                                    // 选择具体配置
                                    ModelsConfig sel = list.get(which - 1);
                                    ModelManager.setSelectedConfigId(sel.getId());
                                    selectedConfigText.setText("Config#" + sel.getId());
                                    // 同步为当前配置对应的模型，保证请求与界面显示的模型一致
                                    api.getModelById(sel.getModelId(), header).enqueue(new Callback<ModelWithEffectiveResponse>() {
                                        @Override
                                        public void onResponse(Call<ModelWithEffectiveResponse> call, Response<ModelWithEffectiveResponse> response) {
                                            if (response.isSuccessful() && response.body() != null && response.body().getModel() != null) {
                                                ModelDto dto = response.body().getModel();
                                                ModelManager.setSelectedModelDto(dto);
                                                ModelManager.setSelectedModel(dto.getModelName());
                                                refreshChatModelDisplay();
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<ModelWithEffectiveResponse> call, Throwable t) { }
                                    });
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    // 如果后端没有按用户列出配置接口，给出更明确的提示
                    int code = response.code();
                    if (code == 404) {
                        Toast.makeText(getContext(), "后端未提供按用户列出配置接口 (404)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "获取配置失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<java.util.List<ModelsConfig>> call, Throwable t) {
                Toast.makeText(getContext(), "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void maybeFetchConfigAndSend(String message) {
        long cfgId = com.example.chat.ModelManager.getSelectedConfigId();
        Log.d("ChatFragment", "maybeFetchConfigAndSend, selectedConfigId=" + cfgId);
        if (cfgId > 0) {
            ModelApiService api = BackendRetrofitClient.getClient().create(ModelApiService.class);
            api.getConfigById(cfgId).enqueue(new Callback<com.example.chat.beans.ModelsConfig>() {
                @Override
                public void onResponse(Call<com.example.chat.beans.ModelsConfig> call, Response<com.example.chat.beans.ModelsConfig> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("ChatFragment", "getConfigById success, configId=" + cfgId);
                        sendMessageToOllamaWithConfig(message, response.body());
                    } else {
                        Log.w("ChatFragment", "getConfigById failed, code=" + response.code() + ", fallback to default send");
                        sendMessageToOllama(message);
                    }
                }

                @Override
                public void onFailure(Call<com.example.chat.beans.ModelsConfig> call, Throwable t) {
                    Log.e("ChatFragment", "getConfigById error: " + t.getMessage() + ", fallback to default send");
                    sendMessageToOllama(message);
                }
            });
        } else {
            Log.d("ChatFragment", "no config selected, use default send");
            sendMessageToOllama(message);
        }
    }

    // 显示创建新对话的对话框
    private void showCreateConversationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("创建新对话");

        // 设置对话框的布局
        final EditText input = new EditText(requireContext());
        input.setHint("请输入对话标题");
        builder.setView(input);

        // 设置确定按钮
        builder.setPositiveButton("创建", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                title = "新对话"; // 如果用户没有输入标题，使用默认标题
            }
            createNewConversation(title);
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // 显示删除确认对话框
    private void showDeleteConfirmationDialog(Conversation conversation) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除对话")
                .setMessage("确定要删除对话 \"" + conversation.getTitle() + "\" 吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteConversation(conversation);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 删除对话
    private void deleteConversation(Conversation conversation) {
        chatApiService.deleteConversation(conversation.getId()).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && Boolean.TRUE.equals(response.body())) {
                    // 从列表中移除对话
                    conversationAdapter.removeConversation(conversation);

                    // 如果删除的是当前对话，清空当前对话
                    if (currentConversationId != null && currentConversationId.equals(conversation.getId())) {
                        currentConversationId = null;
                        conversationTitle.setText("无对话");
                        currentConversationMessages.clear();
                        chatMessageAdapter.notifyDataSetChanged();
                    }

                    Toast.makeText(getContext(), "对话已删除", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "删除对话失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Toast.makeText(getContext(), "删除对话失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 加载用户的所有对话
    private void loadConversations() {
        chatApiService.getConversationsByUser(userId).enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(Call<List<Conversation>> call, Response<List<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversations.clear();
                    conversations.addAll(response.body());
                    conversationAdapter.notifyDataSetChanged();
                    Log.d("ChatFragment", "成功加载对话列表，数量: " + response.body().size());
                } else {
                    String errorMsg = "加载对话列表失败，响应码: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ", 错误信息: " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += ", 解析错误信息失败: " + e.getMessage();
                        }
                    }
                    Log.e("ChatFragment", errorMsg);
                    Toast.makeText(getContext(), "加载对话列表失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Conversation>> call, Throwable t) {
                Log.e("ChatFragment", "加载对话列表失败: " + t.getMessage() + ", userId=" + userId);
                Toast.makeText(getContext(), "加载对话列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 切换对话
    private void switchConversation(Conversation conversation) {
        currentConversationId = conversation.getId();
        conversationTitle.setText(conversation.getTitle());

        // 清空聊天显示
        currentConversationMessages.clear();
        chatMessageAdapter.notifyDataSetChanged();

        // 加载该会话的消息
        loadConversationMessages(currentConversationId);
    }

    // 创建或获取当前会话
    private void createOrGetConversation() {
        // 首先尝试获取用户的最新会话
        chatApiService.getConversationsByUser(userId).enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(Call<List<Conversation>> call, Response<List<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // 使用最新的会话
                    Conversation latestConversation = response.body().get(0);
                    currentConversationId = latestConversation.getId();
                    conversationTitle.setText(latestConversation.getTitle());

                    // 加载该会话的历史消息
                    loadConversationMessages(currentConversationId);
                } else {
                    Toast.makeText(getContext(), "没有现有会话,请创建新会话", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Conversation>> call, Throwable t) {
                Log.e("ChatFragment", "获取会话失败: " + t.getMessage());
                // 创建新会话

            }
        });
    }

    // 创建新会话
    private void createNewConversation(String title) {
        Conversation newConversation = new Conversation(userId, title);
        chatApiService.createConversation(newConversation).enqueue(new Callback<Conversation>() {
            @Override
            public void onResponse(Call<Conversation> call, Response<Conversation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentConversationId = response.body().getId();
                    conversationTitle.setText(title);

                    // 清空聊天显示
                    currentConversationMessages.clear();
                    chatMessageAdapter.notifyDataSetChanged();

                    // 添加到对话列表
                    conversations.add(0, response.body());
                    conversationAdapter.notifyItemInserted(0);

                    Toast.makeText(getContext(), "新对话已创建", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "创建对话失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Conversation> call, Throwable t) {
                Log.e("ChatFragment", "创建会话失败: " + t.getMessage());
                Toast.makeText(getContext(), "创建对话失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 保持原有的无参数方法，但将其重定向到有参数的方法
    private void createNewConversation() {
        createNewConversation("新对话");
    }

    // 加载会话消息
    private void loadConversationMessages(Long conversationId) {
        chatApiService.getMessagesByConversation(conversationId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 清空当前聊天显示
                    currentConversationMessages.clear();

                    // 将历史消息存入全局存储
                    currentConversationMessages.addAll(response.body());
                    chatMessageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("ChatFragment", "加载消息失败: " + t.getMessage());
            }
        });
    }

    // 保存消息到后端
    private void saveMessageToBackend(String content, Boolean isUser, Boolean addToUI) {
        if (currentConversationId == null) {
            Log.e("ChatFragment", "无法保存消息：当前会话ID为空");
            return;
        }

        Message message = new Message(currentConversationId, content, isUser);
        chatApiService.addMessage(message).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 更新本地消息列表（如果需要）
                    if (addToUI) {
                        currentConversationMessages.add(response.body());
                        chatMessageAdapter.notifyItemInserted(currentConversationMessages.size() - 1);
                        scrollToBottom();
                    }
                } else {
                    Log.e("ChatFragment", "保存消息失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Log.e("ChatFragment", "保存消息失败: " + t.getMessage());
            }
        });
    }

    // 保持向后兼容的旧方法
    private void saveMessageToBackend(String content, Boolean isUser) {
        saveMessageToBackend(content, isUser, true);
    }

    private void scrollToBottom() {
        if (rvChatMessages != null && chatMessageAdapter != null && chatMessageAdapter.getItemCount() > 0) {
            rvChatMessages.post(() -> rvChatMessages.smoothScrollToPosition(chatMessageAdapter.getItemCount() - 1));
        }
    }

    private void appendMessage(String sender, String message) {
        Message msg = new Message(currentConversationId, message, true);
        currentConversationMessages.add(msg);
        chatMessageAdapter.notifyItemInserted(currentConversationMessages.size() - 1);
        scrollToBottom();
    }

    private void appendThinkChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        thinkDisplay.append(chunk);
        thinkScrollView.post(() -> thinkScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void startAiMessage() {
        // 添加一个空的AI消息占位
        Message aiMsg = new Message(currentConversationId, "", false);
        currentConversationMessages.add(aiMsg);
        chatMessageAdapter.notifyItemInserted(currentConversationMessages.size() - 1);
        scrollToBottom();
    }

    private void appendAiChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;

        // 更新最后一条AI消息的内容
        if (!currentConversationMessages.isEmpty()) {
            Message lastMsg = currentConversationMessages.get(currentConversationMessages.size() - 1);
            if (!lastMsg.getIsUser()) {
                lastMsg.setContent(lastMsg.getContent() + chunk);
                chatMessageAdapter.notifyItemChanged(currentConversationMessages.size() - 1);
            }
        }
        scrollToBottom();
    }

    private void endAiMessage() {
        scrollToBottom();
    }

    private void sendMessageToOllama(String message) {
        Retrofit retrofit = ChatRetrofitClient.getClient();
        OllamaApiService service = retrofit.create(OllamaApiService.class);

        // 默认配置：仅携带 model / messages / stream=true
        // 构建完整对话历史，避免与 saveMessageToBackend 回调已加入的当前条重复
        List<Message> fullHistory = new ArrayList<>(currentConversationMessages);
        if (fullHistory.isEmpty() || !(fullHistory.get(fullHistory.size() - 1).getIsUser() && message.equals(fullHistory.get(fullHistory.size() - 1).getContent()))) {
            fullHistory.add(new Message(currentConversationId, message, true));
        }

        List<OllamaMessage> messages = new ArrayList<>();
        for (Message msg : fullHistory) {
            String role = msg.getIsUser() ? "user" : "assistant";
            messages.add(new OllamaMessage(role, msg.getContent()));
        }

        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(ModelManager.getSelectedModelOrDefault());
        request.setMessages(messages);
        request.setStream(true);
        // 不设置 system / options，让后端使用默认参数

        Gson gson = new Gson();
        String requestJson = gson.toJson(request);
        Log.d("ChatFragment", "Ollama Request: " + requestJson);
        // 使用 api/chat 端点（流式）
        Call<ResponseBody> call = service.generateChatResponseStream(request);

        startAiMessage();

        // 清空思考区域
        postToUi(() -> thinkDisplay.setText(""));

        // 用于累积AI回复内容
        StringBuilder aiResponse = new StringBuilder();
        // 标记是否已保存AI回复
        AtomicBoolean aiResponseSaved = new AtomicBoolean(false);

        call.enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String err = "";
                    try {
                        if (response.errorBody() != null) {
                            err = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    final String msg = "[响应失败: " + response.code() + "] " + err;
                    postToUi(() -> appendAiChunk(msg));
                    postToUi(() -> endAiMessage());
                    return;
                }


                new Thread(() -> {
                    final String LOG_TAG = "ChatStream";
                    final String THINK_START = "<think>";
                    final String THINK_END = "</think>";

                    boolean inThink = false;
                    StringBuilder buffer = new StringBuilder();
                    StringBuilder pending = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line).append('\n');
                            // 以 braceDepth 拆分 JSON 对象
                            int depth = 0;
                            int objStart = -1;
                            for (int i = 0; i < buffer.length(); i++) {
                                char c = buffer.charAt(i);
                                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                                else if (c == '}') { depth--; }
                                if (depth == 0 && objStart >= 0) {
                                    String jsonStr = buffer.substring(objStart, i + 1);
                                    // 删除已处理部分
                                    buffer.delete(0, i + 1);
                                    i = -1; // 重置扫描
                                    objStart = -1;
                                    try {
                                        JSONObject json = new JSONObject(jsonStr);
                                        String chunk = null;

                                        // 处理两种API响应格式；若有 message.thinking 则进思考区
                                        if (json.has("response")) {
                                            chunk = json.getString("response");
                                        } else if (json.has("message")) {
                                            JSONObject message = json.getJSONObject("message");
                                            if (message.has("thinking")) {
                                                String thinking = message.optString("thinking", "");
                                                if (!thinking.isEmpty()) {
                                                    final String t = thinking;
                                                    postToUi(() -> appendThinkChunk(t));
                                                }
                                            }
                                            if (message.has("content")) {
                                                chunk = message.optString("content", "");
                                            }
                                        }

                                        if (chunk != null && !chunk.isEmpty()) {
                                            aiResponse.append(chunk); // 累积AI回复
                                            pending.append(chunk);
                                            while (true) {
                                                if (inThink) {
                                                    int end = indexOfSafe(pending, THINK_END);
                                                    if (end >= 0) {
                                                        // 输出思考内容直到</think>
                                                        final String thinkText = pending.substring(0, end);
                                                        if (!thinkText.isEmpty()) {
                                                            postToUi(() -> appendThinkChunk(thinkText));
                                                        }
                                                        pending.delete(0, end + THINK_END.length());
                                                        inThink = false;
                                                        continue;
                                                    } else {
                                                        // 没有找到</think>，输出所有内容
                                                        if (pending.length() > 0) {
                                                            final String thinkText = pending.toString();
                                                            postToUi(() -> appendThinkChunk(thinkText));
                                                            pending.setLength(0);
                                                        }
                                                        break;
                                                    }
                                                }

                                                int idxOpen = indexOfSafe(pending, THINK_START);
                                                int idxClose = indexOfSafe(pending, THINK_END);
                                                int next = minNonNeg(idxOpen, idxClose);

                                                if (next < 0) {
                                                    // 没有找到任何标签，输出所有内容
                                                    if (pending.length() > 0) {
                                                        final String toAppend = pending.toString();
                                                        postToUi(() -> appendAiChunk(toAppend));
                                                        pending.setLength(0);
                                                    }
                                                    break;
                                                }

                                                if (next > 0) {
                                                    // 输出标签前的内容
                                                    String out = pending.substring(0, next);
                                                    if (!out.isEmpty()) {
                                                        final String toAppend = out;
                                                        postToUi(() -> appendAiChunk(toAppend));
                                                    }
                                                    pending.delete(0, next);
                                                }

                                                if (startsWithSafe(pending, THINK_START)) {
                                                    // 进入思考模式
                                                    pending.delete(0, THINK_START.length());
                                                    inThink = true;
                                                    postToUi(() -> thinkDisplay.append("[思考开始]\n"));
                                                } else if (startsWithSafe(pending, THINK_END)) {
                                                    // 结束思考模式
                                                    pending.delete(0, THINK_END.length());
                                                    inThink = false;
                                                    postToUi(() -> thinkDisplay.append("\n[思考结束]\n"));
                                                }
                                            }
                                        }

                                        // 检查是否结束
                                        if (json.optBoolean("done", false)) {
                                            // 保存AI回复到后端
                                            if (pending.length() > 0) {
                                                final String aiMsg = pending.toString();
                                                postToUi(() -> appendAiChunk(aiMsg));
                                                // 保存到数据库
                                                saveMessageToBackend(aiMsg, false, false);
                                                aiResponseSaved.set(true);
                                            }
                                            buffer.setLength(0);
                                            break;
                                        }
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "JSON解析错误", e);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        final String msg = "[读取流失败: " + e.toString() + "]";
                        postToUi(() -> appendAiChunk(msg));
                    } finally {
                        // 处理剩余内容
                        if (pending.length() > 0) {
                            if (inThink) {
                                final String thinkText = pending.toString();
                                postToUi(() -> appendThinkChunk(thinkText));
                            } else {
                                final String toAppend = pending.toString();
                                postToUi(() -> appendAiChunk(toAppend));
                            }
                        }

                        // 确保AI回复被保存（如果没有被保存过）
                        if (!aiResponseSaved.get() && aiResponse.length() > 0) {
                            final String aiMsg = aiResponse.toString();
                            saveMessageToBackend(aiMsg, false, false);
                        }

                        postToUi(() -> endAiMessage());
                    }
                }).start();
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                postToUi(() -> {
                    appendAiChunk("[请求失败: " + t.getMessage() + "]");
                    endAiMessage();
                });
            }
        });
    }

    private void sendMessageToOllamaWithConfig(String message, com.example.chat.beans.ModelsConfig cfg) {
        Retrofit retrofit = ChatRetrofitClient.getClient();
        OllamaApiService service = retrofit.create(OllamaApiService.class);

        // 构建消息和选项，避免与 saveMessageToBackend 回调已加入的当前条重复
        List<Message> fullHistory = new ArrayList<>(currentConversationMessages);
        if (fullHistory.isEmpty() || !(fullHistory.get(fullHistory.size() - 1).getIsUser() && message.equals(fullHistory.get(fullHistory.size() - 1).getContent()))) {
            fullHistory.add(new Message(currentConversationId, message, true));
        }

        List<OllamaMessage> messages = new ArrayList<>();
        // 若有 prompt，在 messages 开头插入 role=system 的一条
        String prompt = cfg.getPrompt();
        if (prompt != null && !prompt.isEmpty()) {
            messages.add(new OllamaMessage("system", prompt));
        }
        for (Message msg : fullHistory) {
            String role = msg.getIsUser() ? "user" : "assistant";
            messages.add(new OllamaMessage(role, msg.getContent()));
        }

        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(ModelManager.getSelectedModelOrDefault());
        request.setMessages(messages);
        request.setStream(true);

        // 构建 options
        OllamaChatRequest.Options opts = new OllamaChatRequest.Options();
        if (cfg.getTemperature() != null) {
            opts.setTemperature(cfg.getTemperature().floatValue());
        }
        if (cfg.getTopP() != null) {
            opts.setTop_p(cfg.getTopP().floatValue());
        }
        if (cfg.getTopK() != null) {
            opts.setTop_k(cfg.getTopK());
        }
        if (cfg.getNumCtx() != null) {
            opts.setNum_ctx(cfg.getNumCtx());
        }
        if (cfg.getMaxTokens() != null) {
            opts.setNum_predict(cfg.getMaxTokens());
        }
        request.setOptions(opts);

        Gson gson = new Gson();
        String requestJson = gson.toJson(request);
        Log.d("ChatFragment", "Ollama Request with config: " + requestJson);

        // 使用 api/chat 端点（流式）
        Call<ResponseBody> call = service.generateChatResponseStream(request);

        startAiMessage();

        // 清空思考区域
        postToUi(() -> thinkDisplay.setText(""));

        // 用于累积AI回复内容
        StringBuilder aiResponse = new StringBuilder();
        // 标记是否已保存AI回复
        AtomicBoolean aiResponseSaved = new AtomicBoolean(false);

        call.enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String err = "";
                    try {
                        if (response.errorBody() != null) {
                            err = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    final String msg = "[响应失败: " + response.code() + "] " + err;
                    postToUi(() -> appendAiChunk(msg));
                    postToUi(() -> endAiMessage());
                    return;
                }

                new Thread(() -> {
                    final String LOG_TAG = "ChatStream";
                    final String THINK_START = "<think>";
                    final String THINK_END = "</think>";

                    boolean inThink = false;
                    StringBuilder buffer = new StringBuilder();
                    StringBuilder pending = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line).append('\n');
                            // 以 braceDepth 拆分 JSON 对象
                            int depth = 0;
                            int objStart = -1;
                            for (int i = 0; i < buffer.length(); i++) {
                                char c = buffer.charAt(i);
                                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                                else if (c == '}') { depth--; }
                                if (depth == 0 && objStart >= 0) {
                                    String jsonStr = buffer.substring(objStart, i + 1);
                                    // 删除已处理部分
                                    buffer.delete(0, i + 1);
                                    i = -1; // 重置扫描
                                    objStart = -1;
                                    try {
                                        JSONObject json = new JSONObject(jsonStr);
                                        String chunk = null;

                                        // 处理两种API响应格式；若有 message.thinking 则进思考区
                                        if (json.has("response")) {
                                            chunk = json.getString("response");
                                        } else if (json.has("message")) {
                                            JSONObject message = json.getJSONObject("message");
                                            if (message.has("thinking")) {
                                                String thinking = message.optString("thinking", "");
                                                if (!thinking.isEmpty()) {
                                                    final String t = thinking;
                                                    postToUi(() -> appendThinkChunk(t));
                                                }
                                            }
                                            if (message.has("content")) {
                                                chunk = message.optString("content", "");
                                            }
                                        }

                                        if (chunk != null && !chunk.isEmpty()) {
                                            aiResponse.append(chunk); // 累积AI回复
                                            pending.append(chunk);
                                            while (true) {
                                                if (inThink) {
                                                    int end = indexOfSafe(pending, THINK_END);
                                                    if (end >= 0) {
                                                        // 输出思考内容直到</think>
                                                        final String thinkText = pending.substring(0, end);
                                                        if (!thinkText.isEmpty()) {
                                                            postToUi(() -> appendThinkChunk(thinkText));
                                                        }
                                                        pending.delete(0, end + THINK_END.length());
                                                        inThink = false;
                                                        continue;
                                                    } else {
                                                        // 没有找到</think>，输出所有内容
                                                        if (pending.length() > 0) {
                                                            final String thinkText = pending.toString();
                                                            postToUi(() -> appendThinkChunk(thinkText));
                                                            pending.setLength(0);
                                                        }
                                                        break;
                                                    }
                                                }

                                                int idxOpen = indexOfSafe(pending, THINK_START);
                                                int idxClose = indexOfSafe(pending, THINK_END);
                                                int next = minNonNeg(idxOpen, idxClose);

                                                if (next < 0) {
                                                    // 没有找到任何标签，输出所有内容
                                                    if (pending.length() > 0) {
                                                        final String toAppend = pending.toString();
                                                        postToUi(() -> appendAiChunk(toAppend));
                                                        pending.setLength(0);
                                                    }
                                                    break;
                                                }

                                                if (next > 0) {
                                                    // 输出标签前的内容
                                                    String out = pending.substring(0, next);
                                                    if (!out.isEmpty()) {
                                                        final String toAppend = out;
                                                        postToUi(() -> appendAiChunk(toAppend));
                                                    }
                                                    pending.delete(0, next);
                                                }

                                                if (startsWithSafe(pending, THINK_START)) {
                                                    // 进入思考模式
                                                    pending.delete(0, THINK_START.length());
                                                    inThink = true;
                                                    postToUi(() -> thinkDisplay.append("[思考开始]\n"));
                                                } else if (startsWithSafe(pending, THINK_END)) {
                                                    // 结束思考模式
                                                    pending.delete(0, THINK_END.length());
                                                    inThink = false;
                                                    postToUi(() -> thinkDisplay.append("\n[思考结束]\n"));
                                                }
                                            }
                                        }

                                        // 检查是否结束
                                        if (json.optBoolean("done", false)) {
                                            // 保存AI回复到后端
                                            if (pending.length() > 0) {
                                                final String aiMsg = pending.toString();
                                                postToUi(() -> appendAiChunk(aiMsg));
                                                // 保存到数据库
                                                saveMessageToBackend(aiMsg, false, false);
                                                aiResponseSaved.set(true);
                                            }
                                            buffer.setLength(0);
                                            break;
                                        }
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "JSON解析错误", e);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        final String msg = "[读取流失败: " + e.toString() + "]";
                        postToUi(() -> appendAiChunk(msg));
                    } finally {
                        // 处理剩余内容
                        if (pending.length() > 0) {
                            if (inThink) {
                                final String thinkText = pending.toString();
                                postToUi(() -> appendThinkChunk(thinkText));
                            } else {
                                final String toAppend = pending.toString();
                                postToUi(() -> appendAiChunk(toAppend));
                            }
                        }

                        // 确保AI回复被保存（如果没有被保存过）
                        if (!aiResponseSaved.get() && aiResponse.length() > 0) {
                            final String aiMsg = aiResponse.toString();
                            saveMessageToBackend(aiMsg, false, false);
                        }

                        postToUi(() -> endAiMessage());
                    }
                }).start();
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                postToUi(() -> {
                    appendAiChunk("[请求失败: " + t.getMessage() + "]");
                    endAiMessage();
                });
            }
        });
    }

    private void postToUi(Runnable r) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(r);
        }
    }

    private int indexOfSafe(StringBuilder sb, String s) {
        if (sb == null || s == null || s.isEmpty()) return -1;
        String str = sb.toString();
        return str.indexOf(s);
    }

    private boolean startsWithSafe(StringBuilder sb, String s) {
        if (sb == null || s == null || s.isEmpty() || sb.length() < s.length()) return false;
        return sb.indexOf(s) == 0;
    }

    private int minNonNeg(int a, int b) {
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }

    private static class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {
        private final List<Message> messages;

        ChatMessageAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Message msg = messages.get(position);
            boolean isUser = msg.getIsUser() != null && msg.getIsUser();

            holder.layoutUser.setVisibility(isUser ? View.VISIBLE : View.GONE);
            holder.layoutBot.setVisibility(isUser ? View.GONE : View.VISIBLE);

            if (isUser) {
                holder.tvUserMessage.setText(msg.getContent());
            } else {
                holder.tvBotMessage.setText(msg.getContent());
            }
        }

        @Override
        public int getItemCount() {
            return messages == null ? 0 : messages.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout layoutUser, layoutBot;
            TextView tvUserMessage, tvBotMessage;

            VH(View itemView) {
                super(itemView);
                layoutUser = itemView.findViewById(R.id.layout_user);
                layoutBot = itemView.findViewById(R.id.layout_bot);
                tvUserMessage = itemView.findViewById(R.id.tv_user_message);
                tvBotMessage = itemView.findViewById(R.id.tv_bot_message);
            }
        }
    }
}
