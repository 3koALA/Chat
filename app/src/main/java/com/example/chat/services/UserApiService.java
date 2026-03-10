package com.example.chat.services;

import com.example.chat.beans.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UserApiService {
    @POST("/user/userregist")
    Call<Map<String, Object>> register(@Body User user);

    @POST("/user/login")
    Call<Map<String, Object>> login(@Body User user);

    @GET("user/username/{username}")
    Call<User> getUserByUsername(@Path("username") String username);
}