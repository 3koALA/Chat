package com.example.chat.services;

import com.example.chat.beans.OllamaChatRequest;
import com.example.chat.beans.ShowModelRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface OllamaApiService {
    @Streaming
    @POST("api/chat")
    Call<ResponseBody> generateChatResponseStream(@Body OllamaChatRequest request);

    @POST("api/show")
    Call<ResponseBody> getModelDetails(@Body ShowModelRequest request);
}