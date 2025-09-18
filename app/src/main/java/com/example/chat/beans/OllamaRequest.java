package com.example.chat.beans;


public class OllamaRequest {
    private String model;
    private String prompt;
    private String system;
    private boolean stream;
    private double temperature;
    private double top_p;
    private int max_tokens;

    // 构造函数、getter和setter方法
    public OllamaRequest(String model, String prompt, String system, boolean stream,
                         double temperature, double top_p, int max_tokens) {
        this.model = model;
        this.prompt = prompt;
        this.system = system;
        this.stream = stream;
        this.temperature = temperature;
        this.top_p = top_p;
        this.max_tokens = max_tokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }
}