package com.example.chat.fragments;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class VoiceChatFragment extends Fragment implements TextToSpeech.OnInitListener {

    private EditText messageInput;
    private Button sendButton, speakButton;
    private TextView chatDisplay;
    private TextView thinkDisplay;
    private Button toggleThinkButton;
    private ScrollView scrollView;
    private ScrollView thinkScrollView;
    private TextToSpeech textToSpeech;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_chat, container, false);

        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        speakButton = view.findViewById(R.id.speak_button);
        chatDisplay = view.findViewById(R.id.chat_display);
        thinkDisplay = view.findViewById(R.id.think_display);
        toggleThinkButton = view.findViewById(R.id.toggle_think_button);
        scrollView = view.findViewById(R.id.scroll_view);
        thinkScrollView = view.findViewById(R.id.think_scroll_view);

        // 初始化文本转语音
        textToSpeech = new TextToSpeech(requireContext(), this);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                appendMessage("用户", message);
                messageInput.setText("");
                sendMessageToOllama(message);
            }
        });

        speakButton.setOnClickListener(v -> {
            // 这里可接入语音识别，当前仍使用文本输入
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

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speakButton.setEnabled(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void appendMessage(String sender, String message) {
        String currentText = chatDisplay.getText().toString();
        chatDisplay.setText(currentText + sender + ": " + message + "\n\n");
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

    private void endAiMessage(String finalText) {
        chatDisplay.append("\n\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        if (textToSpeech != null && finalText != null && !finalText.isEmpty()) {
            textToSpeech.speak(finalText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 工具方法：与 ChatFragment 一致
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

    private void sendMessageToOllama(String message) {
        Retrofit retrofit = RetrofitClient.getClient();
        OllamaApiService service = retrofit.create(OllamaApiService.class);

        OllamaRequest request = new OllamaRequest(
                ModelManager.getSelectedModelOrDefault(),
                message,
                "",
                true,
                0.7,
                0.9,
                0
        );

        Call<okhttp3.ResponseBody> call = service.generateResponseStream(request);
        startAiMessage();

        // 清空思考区域
        requireActivity().runOnUiThread(() -> thinkDisplay.setText(""));

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
                    requireActivity().runOnUiThread(() -> {
                        appendAiChunk(msg);
                        endAiMessage("");
                    });
                    return;
                }
                new Thread(() -> {
                    boolean inThink = false;
                    StringBuilder buffer = new StringBuilder();
                    StringBuilder pending = new StringBuilder();
                    final StringBuilder finalText = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line).append('\n');
                            int depth = 0;
                            int objStart = -1;
                            for (int i = 0; i < buffer.length(); i++) {
                                char c = buffer.charAt(i);
                                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                                else if (c == '}') { depth--; }
                                if (depth == 0 && objStart >= 0) {
                                    String jsonStr = buffer.substring(objStart, i + 1);
                                    buffer.delete(0, i + 1);
                                    i = -1;
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
                                                                requireActivity().runOnUiThread(() -> appendThinkChunk(thinkText));
                                                            }
                                                            pending.delete(0, end + "</think>".length());
                                                            inThink = false;
                                                            continue;
                                                        } else {
                                                            // 没有找到</think>，输出所有内容
                                                            if (pending.length() > 0) {
                                                                final String thinkText = pending.toString();
                                                                requireActivity().runOnUiThread(() -> appendThinkChunk(thinkText));
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
                                                            finalText.append(toAppend);
                                                            requireActivity().runOnUiThread(() -> appendAiChunk(toAppend));
                                                            pending.setLength(0);
                                                        }
                                                        break;
                                                    }

                                                    if (next > 0) {
                                                        // 输出标签前的内容
                                                        String out = pending.substring(0, next);
                                                        if (!out.isEmpty()) {
                                                            final String toAppend = out;
                                                            finalText.append(toAppend);
                                                            requireActivity().runOnUiThread(() -> appendAiChunk(toAppend));
                                                        }
                                                        pending.delete(0, next);
                                                    }

                                                    if (startsWithSafe(pending, "<think>")) {
                                                        // 进入思考模式
                                                        pending.delete(0, "<think>".length());
                                                        inThink = true;
                                                        requireActivity().runOnUiThread(() -> thinkDisplay.append("[思考开始]\n"));
                                                    } else if (startsWithSafe(pending, "</think>")) {
                                                        // 结束思考模式
                                                        pending.delete(0, "</think>".length());
                                                        inThink = false;
                                                        requireActivity().runOnUiThread(() -> thinkDisplay.append("\n[思考结束]\n"));
                                                    }
                                                }
                                            }
                                        }
                                        if (json.optBoolean("done", false)) {
                                            buffer.setLength(0);
                                            break;
                                        }
                                    } catch (Exception ignored) { }
                                }
                            }
                        }
                    } catch (Exception e) {
                        final String msg = "[读取流失败: " + e.toString() + "]";
                        requireActivity().runOnUiThread(() -> appendAiChunk(msg));
                    } finally {
                        // 处理剩余内容
                        if (pending.length() > 0) {
                            if (inThink) {
                                final String thinkText = pending.toString();
                                requireActivity().runOnUiThread(() -> appendThinkChunk(thinkText));
                            } else {
                                final String toAppend = pending.toString();
                                finalText.append(toAppend);
                                requireActivity().runOnUiThread(() -> appendAiChunk(toAppend));
                            }
                        }
                        requireActivity().runOnUiThread(() -> endAiMessage(finalText.toString()));
                    }
                }).start();
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    appendAiChunk("[请求失败: " + t.getMessage() + "]");
                    endAiMessage("");
                });
            }
        });
    }
}