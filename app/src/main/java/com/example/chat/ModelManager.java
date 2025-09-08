package com.example.chat;

public final class ModelManager {

    private static volatile String selectedModel;

    private ModelManager() { }

    public static void setSelectedModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) return;
        selectedModel = modelName.trim();
    }

    public static String getSelectedModelOrDefault() {
        return selectedModel != null && !selectedModel.isEmpty() ? selectedModel : "llama2";
    }
}


