package com.example.chat.retrofitclient;

import com.example.chat.BaseUrl;
import com.example.chat.ResponseValidator;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ChatRetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new ResponseValidator())
                    .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时
                    .readTimeout(120, TimeUnit.SECONDS)   // 读取超时
                    .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时
                    .build();

            return new Retrofit.Builder()
                    .baseUrl(BaseUrl.CHAT)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
