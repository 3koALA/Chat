package com.example.chat.beans;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class Conversation {
    private Long id;
    private Long userId;
    private User user;
    private String title;
    private List<Message> messages;
    private String createdAt;
    private String updatedAt;

    // 构造函数、getter和setter
    public Conversation() {}

    public Conversation(Long userId, String title) {
        this.userId = userId;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 添加一个获取Date对象的方法
    public Date getCreatedAtAsDate() {
        try {
            // 假设日期格式为ISO格式：2025-09-17T11:06:18
            if (createdAt != null && !createdAt.isEmpty()) {
                // 如果包含时区信息，可能需要更复杂的解析
                if (createdAt.contains("T")) {
                    return new Date(java.sql.Timestamp.valueOf(createdAt.replace("T", " ")).getTime());
                } else {
                    return new Date(Long.parseLong(createdAt));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
