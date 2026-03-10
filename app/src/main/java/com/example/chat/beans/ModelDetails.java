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

    // 新增：获取 Top-K 参数
    public Integer getTopK() {
        // 尝试从 parsedParameters 获取 top_k
        if (parsedParameters.containsKey("top_k")) {
            return ((Number) parsedParameters.get("top_k")).intValue();
        }
        // 如果没有，尝试从 modelfile 中解析
        if (modelfile != null && modelfile.contains("top_k")) {
            try {
                String[] lines = modelfile.split("\n");
                for (String line : lines) {
                    if (line.contains("top_k")) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("top_k") && i + 1 < parts.length) {
                                return Integer.parseInt(parts[i + 1]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 40; // 默认值
    }

    // 新增：获取 Num Ctx 参数
    public Integer getNumCtx() {
        // 尝试从 parsedParameters 获取 num_ctx
        if (parsedParameters.containsKey("num_ctx")) {
            return ((Number) parsedParameters.get("num_ctx")).intValue();
        }
        // 尝试从 parsedParameters 获取 num_predict（作为备选）
        if (parsedParameters.containsKey("num_predict")) {
            return ((Number) parsedParameters.get("num_predict")).intValue() * 2; // 简单估算
        }
        // 尝试从 modelfile 中解析
        if (modelfile != null && modelfile.contains("num_ctx")) {
            try {
                String[] lines = modelfile.split("\n");
                for (String line : lines) {
                    if (line.contains("num_ctx")) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("num_ctx") && i + 1 < parts.length) {
                                return Integer.parseInt(parts[i + 1]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 2048; // 默认上下文窗口大小
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

    // 从 modelfile 中提取 PARAMETER 块文本（若存在）
    public String getModelFileParameters() {
        if (modelfile != null && modelfile.contains("PARAMETER")) {
            int start = modelfile.indexOf("PARAMETER") + "PARAMETER".length();
            int end = modelfile.indexOf("\"\"\"", start);
            if (end > start) {
                return modelfile.substring(start, end).trim();
            } else {
                // 如果没有三引号结尾，则返回到字符串末尾
                return modelfile.substring(start).trim();
            }
        }
        return null;
    }

    // 新增：获取所有参数的综合方法（方便调试）
    public Map<String, Object> getAllParameters() {
        Map<String, Object> allParams = new HashMap<>();
        allParams.put("temperature", getTemperature());
        allParams.put("top_p", getTopP());
        allParams.put("top_k", getTopK());
        allParams.put("num_ctx", getNumCtx());
        allParams.put("num_predict", getNumPredict());
        allParams.put("system_prompt", getSystemPrompt());
        return allParams;
    }
}