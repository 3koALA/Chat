package com.example.chat.beans;

import java.util.List;

public class OllamaChatRequest {
    private String model;
    private List<OllamaMessage> messages;
    private boolean stream;
    private Options options;



    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<OllamaMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OllamaMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Options {
        private float temperature;
        private float top_p;
        private int max_tokens;

        public Options(float temperature,float top_p ,int max_tokens) {
            this.temperature = temperature;
            this.max_tokens = max_tokens;
            this.top_p = top_p;
        }

        public float getTop_p() {
            return top_p;
        }

        public void setTop_p(float top_p) {
            this.top_p = top_p;
        }

        public float getTemperature() {
            return temperature;
        }

        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }

        public int getMax_tokens() {
            return max_tokens;
        }

        public void setMax_tokens(int max_tokens) {
            this.max_tokens = max_tokens;
        }
    }
}