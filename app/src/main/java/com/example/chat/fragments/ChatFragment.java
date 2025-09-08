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
    private ScrollView scrollView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        chatDisplay = view.findViewById(R.id.chat_display);
        scrollView = view.findViewById(R.id.scroll_view);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                appendMessage("用户", message);
                messageInput.setText("");
                sendMessageToOllama(message);
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

        OllamaRequest request = new OllamaRequest(
                ModelManager.getSelectedModelOrDefault(),
                message,
                "",
                true,   // 开启流式
                0.7,
                0.9,
                0
        );

        Call<ResponseBody> call = service.generateResponseStream(request);
        startAiMessage();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String err = "";
                    try {
                        if (response.errorBody() != null) {
                            err = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    final String msg = "[响应失败: " + response.code() + "] " + err;
                    requireActivity().runOnUiThread(() -> appendAiChunk(msg));
                    requireActivity().runOnUiThread(() -> endAiMessage());
                    return;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        try {
                            JSONObject json = new JSONObject(line);
                            if (json.has("response")) {
                                String chunk = json.getString("response");
                                if (chunk != null && !chunk.isEmpty()) {
                                    // 逐字显示
                                    for (int i = 0; i < chunk.length(); i++) {
                                        final String s = String.valueOf(chunk.charAt(i));
                                        requireActivity().runOnUiThread(() -> appendAiChunk(s));
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // 非 JSON 行忽略
                        }
                    }
                } catch (Exception e) {
                    final String msg = "[读取流失败: " + e.getMessage() + "]";
                    requireActivity().runOnUiThread(() -> appendAiChunk(msg));
                } finally {
                    requireActivity().runOnUiThread(() -> endAiMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    appendAiChunk("[请求失败: " + t.getMessage() + "]");
                    endAiMessage();
                });
            }
        });
    }
}