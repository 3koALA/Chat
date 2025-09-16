package com.example.chat;

import com.example.chat.beans.ModelDetails;

public final class ModelManager {

    private static volatile String selectedModel;
    private static ModelDetails currentModelDetails;

    // 默认值作为后备
    private static String systemPrompt = "";
    private static float temperature = 0.7f;
    private static float topP = 0.9f;
    private static int maxTokens = 128;

    private ModelManager() { }

    public static void setSelectedModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) return;
        selectedModel = modelName.trim();
    }

    public static String getSelectedModelOrDefault() {
        return selectedModel != null && !selectedModel.isEmpty() ? selectedModel : "llama2";
    }

    public static void setModelDetails(ModelDetails details) {
        currentModelDetails = details;
        if (details != null) {
            // 使用从API获取的值更新默认值
            systemPrompt = details.getSystemPrompt();
            temperature = details.getTemperature();
            topP = details.getTopP();
            maxTokens = details.getNumPredict();
        }
    }

    public static ModelDetails getModelDetails() {
        return currentModelDetails;
    }

    public static String getSystemPrompt() {
        return systemPrompt;
    }

    public static void setSystemPrompt(String prompt) {
        systemPrompt = prompt;
    }

    public static float getTemperature() {
        return temperature;
    }

    public static void setTemperature(float temp) {
        temperature = temp;
    }

    public static float getTopP() {
        return topP;
    }

    public static void setTopP(float p) {
        topP = p;
    }

    public static int getMaxTokens() {
        return maxTokens;
    }

    public static void setMaxTokens(int tokens) {
        maxTokens = tokens;
    }
}