package com.example.chat.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.chat.beans.OllamaRequest;
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

public class ChatFragment extends Fragment {

    private EditText messageInput;
    private Button sendButton;
    private TextView chatDisplay;
    private TextView thinkDisplay;
    private Button toggleThinkButton;
    private ScrollView scrollView;
    private ScrollView thinkScrollView;
    private ImageButton historyButton;
    private ImageButton newChatButton;
    private TextView conversationTitle;
    private DrawerLayout drawerLayout;
    private RecyclerView conversationList;
    private Button sidebarNewChatButton;
    private ImageButton closeHistoryButton;

    private Long currentConversationId = null;
    private Long userId;
    private String username;

    private ChatApiService chatApiService;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversations = new ArrayList<>();
    private final List<Message> currentConversationMessages = new ArrayList<>();

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

            // 创建或获取当前会话
            createOrGetConversation();
        }

        // 设置监听器
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        chatDisplay = view.findViewById(R.id.chat_display);
        thinkDisplay = view.findViewById(R.id.think_display);
        toggleThinkButton = view.findViewById(R.id.toggle_think_button);
        scrollView = view.findViewById(R.id.scroll_view);
        thinkScrollView = view.findViewById(R.id.think_scroll_view);
        historyButton = view.findViewById(R.id.history_button);
        newChatButton = view.findViewById(R.id.new_chat_button);
        conversationTitle = view.findViewById(R.id.conversation_title);
        drawerLayout = view.findViewById(R.id.drawer_layout);
        conversationList = view.findViewById(R.id.conversation_list);
        sidebarNewChatButton = view.findViewById(R.id.sidebar_new_chat_button);
        closeHistoryButton = view.findViewById(R.id.close_history_button);
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
                appendMessage("用户", message);
                messageInput.setText("");

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

                                // 保存用户消息到后端
                                saveMessageToBackend(message, true);

                                // 发送到Ollama
                                sendMessageToOllama(message);
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
                    // 已有会话，直接保存消息和发送
                    saveMessageToBackend(message, true);
                    sendMessageToOllama(message);
                }
            }
        });

        toggleThinkButton.setOnClickListener(v -> {
            if (thinkScrollView.getVisibility() == View.VISIBLE) {
                thinkScrollView.setVisibility(View.GONE);
                toggleThinkButton.setText("显示思考");
            } else {
                thinkScrollView.setVisibility(View.VISIBLE);
                toggleThinkButton.setText("隐藏思考");
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
                        chatDisplay.setText("");
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
        chatDisplay.setText("");

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
                    chatDisplay.setText("");

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
                    chatDisplay.setText("");

                    // 显示历史消息
                    for (Message message : response.body()) {
                        String sender = message.getIsUser() ? "用户" : "AI";
                        appendMessage(sender, message.getContent());
                    }

                    // 将历史消息存入全局存储
                    currentConversationMessages.clear();
                    currentConversationMessages.addAll(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("ChatFragment", "加载消息失败: " + t.getMessage());
            }
        });
    }

    // 保存消息到后端
    private void saveMessageToBackend(String content, Boolean isUser) {
        if (currentConversationId == null) {
            Log.e("ChatFragment", "无法保存消息：当前会话ID为空");
            return;
        }

        Message message = new Message(currentConversationId, content, isUser);
        chatApiService.addMessage(message).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 更新本地消息列表
                    currentConversationMessages.add(response.body());
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

    private void appendMessage(String sender, String message) {
        String currentText = chatDisplay.getText().toString();
        chatDisplay.setText(currentText + sender + ": " + message + "\n\n");

        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void appendThinkChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        thinkDisplay.append(chunk);
        thinkScrollView.post(() -> thinkScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void startAiMessage() {
        chatDisplay.append("AI: ");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void appendAiChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;

        // 追加到现有内容
        String currentContent = chatDisplay.getText().toString();
        chatDisplay.setText(currentContent + chunk);

        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void endAiMessage() {
        chatDisplay.append("\n\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessageToOllama(String message) {
        Retrofit retrofit = ChatRetrofitClient.getClient();
        OllamaApiService service = retrofit.create(OllamaApiService.class);

        // 获取模型默认参数
        com.example.chat.beans.ModelDetails modelDetails = ModelManager.getModelDetails();
        // 构建完整对话历史
        List<Message> fullHistory = new ArrayList<>(currentConversationMessages);
        fullHistory.add(new Message(currentConversationId, message, true));

        // 确定使用默认参数还是自定义参数
        String systemPrompt = ModelManager.getSystemPrompt();
        float temperature = ModelManager.getTemperature();
        float topP = ModelManager.getTopP();
        int maxTokens = ModelManager.getMaxTokens();

        // 检查是否使用了自定义参数（与默认值不同）
        boolean useCustomParams = false;
        if (modelDetails != null) {
            useCustomParams =
                    !systemPrompt.equals(modelDetails.getSystemPrompt()) ||
                            temperature != modelDetails.getTemperature() ||
                            topP != modelDetails.getTopP() ||
                            maxTokens != modelDetails.getNumPredict();
        }

        // 如果没有自定义参数，使用空字符串作为系统提示词（让模型使用自己的默认提示词）
        // 如果有自定义参数，使用用户设置的系统提示词
        List<OllamaMessage> messages = new ArrayList<>();
        String finalSystemPrompt = useCustomParams ? systemPrompt : "";

//        messages.add(new OllamaMessage("system", finalSystemPrompt)); //有关系统提示词的添加

        for (Message msg : fullHistory) {
            String role = msg.getIsUser() ? "user" : "assistant";
            messages.add(new OllamaMessage(role, msg.getContent()));
        }

        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(ModelManager.getSelectedModelOrDefault());
        request.setMessages(messages); // 关键：传递完整对话历史
        request.setStream(true);
//        request.setSystemPrompt(ModelManager.getSystemPrompt());
        OllamaChatRequest.Options options = new OllamaChatRequest.Options(ModelManager.getTemperature(),ModelManager.getTopP(),ModelManager.getMaxTokens());
        request.setOptions(options);


        Gson gson = new Gson();
        String requestJson = gson.toJson(request);
        Log.d("ChatFragment", "Ollama Request: " + requestJson);
        // 使用api/chat端点
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

                                        // 处理两种API响应格式
                                        if (json.has("response")) {
                                            // /api/generate 格式
                                            chunk = json.getString("response");
                                        } else if (json.has("message")) {
                                            // /api/chat 格式
                                            JSONObject message = json.getJSONObject("message");
                                            if (message.has("content")) {
                                                chunk = message.getString("content");
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
                                                saveMessageToBackend(aiMsg, false);
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
                            saveMessageToBackend(aiMsg, false);
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

    // 工具方法：安全匹配与查找，支持 CharSequence（StringBuilder 也实现了这接口）
    private static boolean startsWithSafe(CharSequence s, String token) {
        if (s == null || token == null) return false;
        if (s.length() < token.length()) return false;
        for (int i = 0; i < token.length(); i++) {
            if (s.charAt(i) != token.charAt(i)) return false;
        }
        return true;
    }

    private static int indexOfSafe(CharSequence s, String token) {
        if (s == null || token == null || token.isEmpty()) return -1;
        for (int i = 0; i + token.length() <= s.length(); i++) {
            boolean found = true;
            for (int j = 0; j < token.length(); j++) {
                if (s.charAt(i + j) != token.charAt(j)) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    private static int minNonNeg(int... vals) {
        int res = -1;
        for (int v : vals) {
            if (v >= 0 && (res < 0 || v < res)) res = v;
        }
        return res;
    }

    // 安全地在主线程更新 UI，避免 Fragment 分离导致的崩溃
    private void postToUi(Runnable action) {
        if (!isAdded()) return;
        final android.app.Activity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (!isAdded()) return;
            action.run();
        });
    }

    // 对话列表适配器
    private static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

        private List<Conversation> conversations;
        private OnConversationClickListener listener;

        public interface OnConversationClickListener {
            void onConversationClick(Conversation conversation);
            void onDeleteConversation(Conversation conversation);
        }

        public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
            this.conversations = conversations;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Conversation conversation = conversations.get(position);
            holder.title.setText(conversation.getTitle());

            // 格式化日期 - 直接使用字符串日期
            String dateStr = "";
            if (conversation.getCreatedAt() != null) {
                try {
                    // 尝试解析ISO格式的日期字符串
                    if (conversation.getCreatedAt().contains("T")) {
                        // ISO格式：2025-09-17T11:06:18 -> 转换为 2025-09-17 11:06
                        String isoDate = conversation.getCreatedAt();
                        String datePart = isoDate.split("T")[0];
                        String timePart = isoDate.split("T")[1].substring(0, 5); // 取前5个字符（HH:mm）
                        dateStr = datePart + " " + timePart;
                    } else {
                        // 可能是时间戳，尝试解析
                        long timestamp = Long.parseLong(conversation.getCreatedAt());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        dateStr = sdf.format(new Date(timestamp));
                    }
                } catch (Exception e) {
                    // 如果解析失败，直接显示原始字符串
                    dateStr = conversation.getCreatedAt();
                }
            }
            holder.date.setText(dateStr);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation);
                }
            });

            // 设置删除按钮点击事件
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteConversation(conversation);
                }
            });
        }

        @Override
        public int getItemCount() {
            return conversations.size();
        }

        // 添加删除对话的方法
        public void removeConversation(Conversation conversation) {
            int position = conversations.indexOf(conversation);
            if (position != -1) {
                conversations.remove(position);
                notifyItemRemoved(position);
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView date;
            ImageButton deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.conversation_title);
                date = itemView.findViewById(R.id.conversation_date);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
}