package com.example.chat.services;

import com.example.chat.beans.ModelsResponse;
import com.example.chat.beans.OllamaChatRequest;
import com.example.chat.beans.OllamaRequest;
import com.example.chat.beans.OllamaResponse;
import com.example.chat.beans.ShowModelRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface OllamaApiService {
    @GET("api/tags")
    Call<ModelsResponse> getModels();

    @Streaming
    @POST("api/chat")
    Call<ResponseBody> generateChatResponseStream(@Body OllamaChatRequest request);

    @Streaming
    @POST("api/generate")
    Call<ResponseBody> generateResponseStream(@Body OllamaRequest request);

    // 新增获取模型详情的方法
    @POST("api/show")
    Call<ResponseBody> getModelDetails(@Body ShowModelRequest request);
}