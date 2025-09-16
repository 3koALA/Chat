package com.example.chat.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.chat.ModelManager;
import com.example.chat.R;

import com.example.chat.beans.ModelDetails;
import org.json.JSONObject;

import com.example.chat.retrofitclient.ChatRetrofitClient;
import com.example.chat.beans.ModelInfo;
import com.example.chat.beans.ModelsResponse;
import com.example.chat.beans.ShowModelRequest;
import com.example.chat.services.OllamaApiService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectionFragment extends Fragment {
    private OllamaApiService ollamaApiService;

    private Spinner modelSpinner;
    private Button refreshButton;
    private Button settingsButton;
    private List<String> models = new ArrayList<>();

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ModelSettings";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_TOP_P = "top_p";
    private static final String KEY_MAX_TOKENS = "max_tokens";

    private String currentSystemPrompt = "";
    private float currentTemperature = 0.7f;
    private float currentTopP = 0.9f;
    private int currentMaxTokens = 128;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_model_selection, container, false);

        ollamaApiService = ChatRetrofitClient.getClient().create(OllamaApiService.class);

        // 获取布局中的组件
        modelSpinner = view.findViewById(R.id.model_spinner);
        refreshButton = view.findViewById(R.id.refresh_button);
        settingsButton = view.findViewById(R.id.settings_button);

        // 初始化SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);

        // 加载保存的设置
        loadSavedSettings();

        refreshButton.setOnClickListener(v -> fetchModels());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        settingsButton.setEnabled(false); // 初始时禁用，直到加载模型

        // 保存用户选择
        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < models.size()) {
                    String modelName = models.get(position);
                    ModelManager.setSelectedModel(modelName);
                    // 获取模型详细信息
                    fetchModelDetails(modelName);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // 初始加载模型
        fetchModels();

        return view;
    }

    private void loadSavedSettings() {
        currentSystemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "");
        currentTemperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f);
        currentTopP = prefs.getFloat(KEY_TOP_P, 0.9f);
        currentMaxTokens = prefs.getInt(KEY_MAX_TOKENS, 128);

        // 更新ModelManager中的设置
        ModelManager.setSystemPrompt(currentSystemPrompt);
        ModelManager.setTemperature(currentTemperature);
        ModelManager.setTopP(currentTopP);
        ModelManager.setMaxTokens(currentMaxTokens);
    }

    private void saveSettings(String systemPrompt, float temperature, float topP, int maxTokens) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SYSTEM_PROMPT, systemPrompt);
        editor.putFloat(KEY_TEMPERATURE, temperature);
        editor.putFloat(KEY_TOP_P, topP);
        editor.putInt(KEY_MAX_TOKENS, maxTokens);
        editor.apply();

        // 更新当前设置和ModelManager
        currentSystemPrompt = systemPrompt;
        currentTemperature = temperature;
        currentTopP = topP;
        currentMaxTokens = maxTokens;

        ModelManager.setSystemPrompt(systemPrompt);
        ModelManager.setTemperature(temperature);
        ModelManager.setTopP(topP);
        ModelManager.setMaxTokens(maxTokens);

        Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private void resetSettings() {
        // 重置为默认值
        saveSettings("", 0.7f, 0.9f, 128);
        Toast.makeText(requireContext(), "设置已重置", Toast.LENGTH_SHORT).show();
    }

    private void updateSettingsFromModelDefaults(ModelDetails modelDetails) {
        if (modelDetails != null) {
            // 使用从API获取的值更新设置
            currentSystemPrompt = modelDetails.getSystemPrompt();
            currentTemperature = modelDetails.getTemperature();
            currentTopP = modelDetails.getTopP();
            currentMaxTokens = modelDetails.getNumPredict();

            // 保存到SharedPreferences
            saveSettings(currentSystemPrompt, currentTemperature, currentTopP, currentMaxTokens);
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_model_settings, null);
        builder.setView(dialogView);

        EditText systemPromptEdit = dialogView.findViewById(R.id.system_prompt_edit);
        SeekBar temperatureSeek = dialogView.findViewById(R.id.temperature_seek);
        TextView temperatureValue = dialogView.findViewById(R.id.temperature_value);
        SeekBar topPSeek = dialogView.findViewById(R.id.top_p_seek);
        TextView topPValue = dialogView.findViewById(R.id.top_p_value);
        EditText maxTokensEdit = dialogView.findViewById(R.id.max_tokens_edit);
//        Button resetButton = dialogView.findViewById(R.id.reset_button);
        Button useModelDefaultsButton = dialogView.findViewById(R.id.reset_button);

        // 设置当前值
        systemPromptEdit.setText(currentSystemPrompt);
        temperatureSeek.setProgress((int)(currentTemperature * 100));
        temperatureValue.setText(String.format("%.2f", currentTemperature));
        topPSeek.setProgress((int)(currentTopP * 100));
        topPValue.setText(String.format("%.2f", currentTopP));
        maxTokensEdit.setText(String.valueOf(currentMaxTokens));

        // 温度滑动条监听
        temperatureSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                temperatureValue.setText(String.format("%.2f", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Top-P滑动条监听
        topPSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                topPValue.setText(String.format("%.2f", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 重置按钮
//        resetButton.setOnClickListener(v -> {
//            systemPromptEdit.setText("");
//            temperatureSeek.setProgress(70);
//            temperatureValue.setText("0.70");
//            topPSeek.setProgress(90);
//            topPValue.setText("0.90");
//            maxTokensEdit.setText("128");
//        });

        // 使用模型默认值按钮
        useModelDefaultsButton.setOnClickListener(v -> {
            ModelDetails modelDetails = ModelManager.getModelDetails();
            if (modelDetails != null) {
                systemPromptEdit.setText(modelDetails.getSystemPrompt());
                temperatureSeek.setProgress((int)(modelDetails.getTemperature() * 100));
                temperatureValue.setText(String.format("%.2f", modelDetails.getTemperature()));
                topPSeek.setProgress((int)(modelDetails.getTopP() * 100));
                topPValue.setText(String.format("%.2f", modelDetails.getTopP()));
                maxTokensEdit.setText(String.valueOf(modelDetails.getNumPredict()));
            } else {
                Toast.makeText(requireContext(), "未获取到模型默认设置", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setPositiveButton("确认", (dialog, which) -> {
            String systemPrompt = systemPromptEdit.getText().toString();
            float temperature = temperatureSeek.getProgress() / 100f;
            float topP = topPSeek.getProgress() / 100f;
            int maxTokens;

            try {
                maxTokens = Integer.parseInt(maxTokensEdit.getText().toString());
            } catch (NumberFormatException e) {
                maxTokens = 128;
            }

            saveSettings(systemPrompt, temperature, topP, maxTokens);
        });

        builder.setNegativeButton("取消", null);

        builder.setTitle("模型参数设置");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void fetchModels() {
        Call<ModelsResponse> call = ollamaApiService.getModels();
        call.enqueue(new Callback<ModelsResponse>() {
            @Override
            public void onResponse(Call<ModelsResponse> call, Response<ModelsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ModelsResponse modelsResponse = response.body();
                    models.clear();

                    for (ModelInfo model : modelsResponse.getModels()) {
                        models.add(model.getName());
                    }

                    // 更新 UI
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            models
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modelSpinner.setAdapter(adapter);

                    if (!models.isEmpty()) {
                        modelSpinner.setSelection(0);
                        String firstModel = models.get(0);
                        ModelManager.setSelectedModel(firstModel);
                        fetchModelDetails(firstModel);
                        settingsButton.setEnabled(true);
                    }

                    Toast.makeText(requireContext(), "模型加载成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "获取模型失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModelsResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 修改 fetchModelDetails 方法
    private void fetchModelDetails(String modelName) {
        ShowModelRequest request = new ShowModelRequest(modelName);
        Call<ResponseBody> call = ollamaApiService.getModelDetails(request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        ModelDetails modelDetails = ModelDetails.fromJson(jsonResponse);

                        ModelManager.setModelDetails(modelDetails);
                        updateSettingsFromModelDefaults(modelDetails);
                        Toast.makeText(requireContext(), "已加载 " + ModelManager.getSelectedModelOrDefault() + " 的默认设置", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "解析模型详情失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "获取模型详情失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}