package com.example.chat.beans;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
}