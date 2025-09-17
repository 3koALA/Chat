package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.beans.User;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.DatabaseApiService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView registerText;
    private TextView guestLoginText;
    private ProgressBar progressBar;

    private DatabaseApiService databaseApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图
        initViews();

        // 初始化数据库API服务
        databaseApiService = BackendRetrofitClient.getClient().create(DatabaseApiService.class);

        // 设置点击监听器
        setupClickListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);
        guestLoginText = findViewById(R.id.guestLoginText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> attemptLogin());

        // 注册文本点击事件
        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 游客登录点击事件
        guestLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEditText.setError("用户名不能为空");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("密码不能为空");
            return;
        }

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 创建用户对象
        User user = new User(username, password);

        // 调用登录API
        Call<Map<String,Object>> call = databaseApiService.login(user);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    System.out.println("Response: " + result); // 打印整个响应
                    Boolean success = (Boolean) result.get("success");
                    String message = (String) result.get("message");
                    // 安全地获取userId
                    Object userIdObj = result.get("userId");
                    Long userId = null;

                    if (userIdObj != null) {
                        if (userIdObj instanceof Integer) {
                            userId = ((Integer) userIdObj).longValue();
                        } else if (userIdObj instanceof Long) {
                            userId = (Long) userIdObj;
                        } else if (userIdObj instanceof Double) {
                            userId = ((Double) userIdObj).longValue();
                        } else if (userIdObj instanceof String) {
                            try {
                                userId = Long.parseLong((String) userIdObj);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    System.out.println("--loginID " + userId);
                    if (success != null && success) {



                        // 登录成功，跳转到主界面并传递用户信息
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("username", username);

                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}