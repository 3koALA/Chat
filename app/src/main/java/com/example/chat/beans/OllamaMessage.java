package com.example.chat.beans;

public class OllamaMessage {
    private String role; // 对应API的role字段
    private String content;
    // 构造函数
    public OllamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    // Getter和Setter
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}