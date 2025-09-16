package com.example.chat.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.chat.ModelManager;
import com.example.chat.services.OllamaApiService;
import com.example.chat.beans.OllamaRequest;
import com.example.chat.R;
import com.example.chat.RetrofitClient;

import org.json.JSONObject;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        chatDisplay = view.findViewById(R.id.chat_display);
        thinkDisplay = view.findViewById(R.id.think_display);
        toggleThinkButton = view.findViewById(R.id.toggle_think_button);
        scrollView = view.findViewById(R.id.scroll_view);
        thinkScrollView = view.findViewById(R.id.think_scroll_view);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                appendMessage("用户", message);
                messageInput.setText("");
                sendMessageToOllama(message);
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

        return view;
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
        chatDisplay.append(chunk);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void endAiMessage() {
        chatDisplay.append("\n\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessageToOllama(String message) {
        Retrofit retrofit = RetrofitClient.getClient();
        OllamaApiService service = retrofit.create(OllamaApiService.class);

        // 获取模型默认参数
        com.example.chat.beans.ModelDetails modelDetails = ModelManager.getModelDetails();

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
        String finalSystemPrompt = useCustomParams ? systemPrompt : "";

        OllamaRequest request = new OllamaRequest(
                ModelManager.getSelectedModelOrDefault(),
                message,
                finalSystemPrompt, // 使用确定的系统提示词
                true,   // 开启流式
                temperature, // 使用确定的温度值
                topP,       // 使用确定的top_p值
                maxTokens   // 使用确定的最大token数
        );

        Call<okhttp3.ResponseBody> call = service.generateResponseStream(request);
        startAiMessage();

        // 清空思考区域
        postToUi(() -> thinkDisplay.setText(""));

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
                                        if (json.has("response")) {
                                            String chunk = json.getString("response");
                                            if (chunk != null && !chunk.isEmpty()) {
                                                pending.append(chunk);
                                                while (true) {
                                                    if (inThink) {
                                                        int end = indexOfSafe(pending, "</think>");
                                                        if (end >= 0) {
                                                            // 输出思考内容直到</think>
                                                            final String thinkText = pending.substring(0, end);
                                                            if (!thinkText.isEmpty()) {
                                                                postToUi(() -> appendThinkChunk(thinkText));
                                                            }
                                                            pending.delete(0, end + "</think>".length());
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

                                                    int idxOpen = indexOfSafe(pending, "<think>");
                                                    int idxClose = indexOfSafe(pending, "</think>");
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

                                                    if (startsWithSafe(pending, "<think>")) {
                                                        // 进入思考模式
                                                        pending.delete(0, "<think>".length());
                                                        inThink = true;
                                                        postToUi(() -> thinkDisplay.append("[思考开始]\n"));
                                                    } else if (startsWithSafe(pending, "</think>")) {
                                                        // 结束思考模式
                                                        pending.delete(0, "</think>".length());
                                                        inThink = false;
                                                        postToUi(() -> thinkDisplay.append("\n[思考结束]\n"));
                                                    }
                                                }
                                            }
                                        }
                                        if (json.optBoolean("done", false)) {
                                            // 读完即可让外层循环结束
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
        outer: for (int i = 0; i + token.length() <= s.length(); i++) {
            for (int j = 0; j < token.length(); j++) {
                if (s.charAt(i + j) != token.charAt(j)) continue outer;
            }
            return i;
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
}