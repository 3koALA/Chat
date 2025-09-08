package com.example.chat.beans;

import java.util.List;

public class ModelsResponse {
    private List<ModelInfo> models;

    // 构造函数、getter和setter方法
    public ModelsResponse(List<ModelInfo> models) {
        this.models = models;
    }

    public List<ModelInfo> getModels() {
        return models;
    }

    public void setModels(List<ModelInfo> models) {
        this.models = models;
    }
}