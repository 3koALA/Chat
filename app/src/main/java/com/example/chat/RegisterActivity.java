package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.beans.User;
import com.example.chat.retrofitclient.BackendRetrofitClient;
import com.example.chat.services.UserApiService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private TextInputEditText nicknameEditText;
    private Button registerButton;
    private ProgressBar progressBar;

    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化视图
        initViews();

        // 初始化数据库API服务
        userApiService = BackendRetrofitClient.getClient().create(UserApiService.class);

        // 设置点击监听器
        setupClickListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        nicknameEditText = findViewById(R.id.nicknameEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String nickName = nicknameEditText.getText().toString().trim();

        // 验证输入
        if (username.isEmpty()) {
            usernameEditText.setError("用户名不能为空");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("密码不能为空");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("密码不一致");
            return;
        }

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 创建用户对象
        User user = new User(username, password,nickName);

        // 调用注册API
        Call<Map<String, Object>> call = userApiService.register(user);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");
                    String message = (String) result.get("message");

                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();

                    if (success != null && success) {
                        // 注册成功，跳转到登录界面
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}