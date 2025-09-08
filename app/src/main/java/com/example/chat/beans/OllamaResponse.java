package com.example.chat.beans;

public class OllamaResponse {
    private String model;
    private String response;

    // 构造函数、getter和setter方法
    public OllamaResponse(String model, String response) {
        this.model = model;
        this.response = response;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

}