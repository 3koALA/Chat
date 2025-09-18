package com.example.chat;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class ResponseValidator implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (!response.isSuccessful()) {
            throw new IOException("API请求失败: " + response.code());
        }
        return response;
    }
}