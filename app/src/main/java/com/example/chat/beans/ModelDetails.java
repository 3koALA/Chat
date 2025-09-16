package com.example.chat.beans;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ModelDetails {
    private String modelfile;
    private String parameters;
    private String template;
    private Map<String, Object> details;
    private Map<String, Object> parsedParameters;

    public ModelDetails() {
        this.details = new HashMap<>();
        this.parsedParameters = new HashMap<>();
    }

    public static ModelDetails fromJson(JSONObject json) throws JSONException {
        ModelDetails details = new ModelDetails();
        details.setModelfile(json.optString("modelfile"));
        details.setParameters(json.optString("parameters"));
        details.setTemplate(json.optString("template"));

        // 解析 details 字段
        if (json.has("details")) {
            JSONObject detailsObj = json.getJSONObject("details");
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("parent_model", detailsObj.optString("parent_model"));
            detailsMap.put("format", detailsObj.optString("format"));
            detailsMap.put("family", detailsObj.optString("family"));
            detailsMap.put("families", detailsObj.optJSONArray("families"));
            detailsMap.put("parameter_size", detailsObj.optString("parameter_size"));
            detailsMap.put("quantization_level", detailsObj.optString("quantization_level"));
            details.setDetails(detailsMap);
        }

        // 解析参数字符串
        String paramsStr = details.getParameters();
        if (paramsStr != null && !paramsStr.isEmpty()) {
            Map<String, Object> parsedParams = new HashMap<>();
            String[] lines = paramsStr.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // 尝试将值转换为适当的数据类型
                    try {
                        if (value.contains(".")) {
                            parsedParams.put(key, Float.parseFloat(value));
                        } else {
                            parsedParams.put(key, Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        parsedParams.put(key, value);
                    }
                }
            }
            details.setParsedParameters(parsedParams);
        }

        return details;
    }

    // Getters and setters
    public String getModelfile() {
        return modelfile;
    }

    public void setModelfile(String modelfile) {
        this.modelfile = modelfile;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Map<String, Object> getParsedParameters() {
        return parsedParameters;
    }

    public void setParsedParameters(Map<String, Object> parsedParameters) {
        this.parsedParameters = parsedParameters;
    }

    // 获取特定参数的便捷方法
    public float getTemperature() {
        return parsedParameters.containsKey("temperature") ?
                ((Number) parsedParameters.get("temperature")).floatValue() : 0.8f;
    }

    public float getTopP() {
        return parsedParameters.containsKey("top_p") ?
                ((Number) parsedParameters.get("top_p")).floatValue() : 0.9f;
    }

    public int getNumPredict() {
        return parsedParameters.containsKey("num_predict") ?
                ((Number) parsedParameters.get("num_predict")).intValue() : 128;
    }

    public String getSystemPrompt() {
        // 从 modelfile 中提取 SYSTEM 提示词
        if (modelfile != null && modelfile.contains("SYSTEM")) {
            int start = modelfile.indexOf("SYSTEM") + 6;
            int end = modelfile.indexOf("\"\"\"", start);
            if (end > start) {
                return modelfile.substring(start, end).trim();
            }
        }
        return "";
    }
}