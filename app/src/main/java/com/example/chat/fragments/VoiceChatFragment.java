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
    private ScrollView scrollView;
    private TextToSpeech textToSpeech;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_chat, container, false);

        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        speakButton = view.findViewById(R.id.speak_button);
        chatDisplay = view.findViewById(R.id.chat_display);
        scrollView = view.findViewById(R.id.scroll_view);

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

        Call<ResponseBody> call = service.generateResponseStream(request);
        startAiMessage();

        final StringBuilder finalText = new StringBuilder();

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
                    requireActivity().runOnUiThread(() -> {
                        appendAiChunk(msg);
                        endAiMessage("");
                    });
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
                                    finalText.append(chunk);
                                    for (int i = 0; i < chunk.length(); i++) {
                                        final String s = String.valueOf(chunk.charAt(i));
                                        requireActivity().runOnUiThread(() -> appendAiChunk(s));
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                    final String msg = "[读取流失败: " + e.getMessage() + "]";
                    requireActivity().runOnUiThread(() -> appendAiChunk(msg));
                } finally {
                    requireActivity().runOnUiThread(() -> endAiMessage(finalText.toString()))
;                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                requireActivity().runOnUiThread(() -> {
                    appendAiChunk("[请求失败: " + t.getMessage() + "]");
                    endAiMessage("");
                });
            }
        });
    }
}