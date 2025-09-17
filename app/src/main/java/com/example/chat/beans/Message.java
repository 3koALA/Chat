package com.example.chat.beans;

import java.time.LocalDateTime;

public class Message {
    private Long id;
    private Long conversationId;
    private Conversation conversation;
    private String content;
    private Boolean isUser;

    // 构造函数、getter和setter
    public Message() {}

    public Message(Long conversationId, String content, Boolean isUser) {
        this.conversationId = conversationId;
        this.content = content;
        this.isUser = isUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsUser() {
        return isUser;
    }

    public void setIsUser(Boolean user) {
        isUser = user;
    }


}
