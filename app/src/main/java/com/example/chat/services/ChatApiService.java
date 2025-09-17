package com.example.chat.services;

import com.example.chat.beans.Conversation;
import com.example.chat.beans.Message;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ChatApiService {
    // 获取用户的所有会话
    @GET("/conversations/user/{userId}")
    Call<List<Conversation>> getConversationsByUser(@Path("userId") Long userId);

    // 获取特定会话的详细信息（包含消息）
    @GET("/conversations/{id}")
    Call<Conversation> getConversation(@Path("id") Long id);

    // 创建新会话
    @POST("/conversations")
    Call<Conversation> createConversation(@Body Conversation conversation);

    // 删除会话
    @DELETE("/conversations/{id}")
    Call<Boolean> deleteConversation(@Path("id") Long id);

    // 获取会话的所有消息
    @GET("/messages/conversation/{conversationId}")
    Call<List<Message>> getMessagesByConversation(@Path("conversationId") Long conversationId);

    // 发送消息
    @POST("/messages")
    Call<Message> addMessage(@Body Message message);


}