package com.example.chat.beans;

import java.util.List;

public class OllamaChatRequest {
    private String model;
    private List<OllamaMessage> messages;
    private String system;
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
        private Float temperature;
        private Float top_p;
        private Integer top_k;
        private Integer num_ctx;
        private Integer num_predict;
        private Integer max_tokens;

        public Options() {}

        public Options(Float temperature, Float top_p, Integer max_tokens) {
            this.temperature = temperature;
            this.top_p = top_p;
            this.max_tokens = max_tokens;
        }

        public Float getTop_p() {
            return top_p;
        }

        public void setTop_p(Float top_p) {
            this.top_p = top_p;
        }

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        public Integer getMax_tokens() {
            return max_tokens;
        }

        public void setMax_tokens(Integer max_tokens) {
            this.max_tokens = max_tokens;
        }

        public Integer getTop_k() {
            return top_k;
        }

        public void setTop_k(Integer top_k) {
            this.top_k = top_k;
        }

        public Integer getNum_ctx() {
            return num_ctx;
        }

        public void setNum_ctx(Integer num_ctx) {
            this.num_ctx = num_ctx;
        }

        public Integer getNum_predict() {
            return num_predict;
        }

        public void setNum_predict(Integer num_predict) {
            this.num_predict = num_predict;
        }
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }
}
