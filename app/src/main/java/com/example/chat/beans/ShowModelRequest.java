package com.example.chat.beans;

public class ShowModelRequest {
    private String name;

    public ShowModelRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}