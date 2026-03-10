package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminChooseActivity extends AppCompatActivity {

    private Button userModeButton;
    private Button adminModeButton;
    private Button backToLoginButton;
    private TextView welcomeText;

    private Long userId;
    private String username;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_choose);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 获取传递的用户信息
        Intent intent = getIntent();
        userId = intent.getLongExtra("userId", -1);
        username = intent.getStringExtra("username");
        role = intent.getStringExtra("role");

        // 初始化视图
        initViews();

        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        userModeButton = findViewById(R.id.userModeButton);
        adminModeButton = findViewById(R.id.adminModeButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);
        welcomeText = findViewById(R.id.welcomeText);

        // 设置欢迎语
        if (username != null) {
            welcomeText.setText("欢迎 " + username + "，请选择模式");
        }
    }

    private void setupClickListeners() {
        // 用户模式按钮点击事件 - 进入普通用户主页
        userModeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminChooseActivity.this, MainActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            intent.putExtra("mode", "user"); // 标记为用户模式
            startActivity(intent);
            finish();
        });

        // 管理员模式按钮点击事件 - 进入管理员页面选择页
        adminModeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminChooseActivity.this, AdminPageSelectActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            intent.putExtra("mode", "admin"); // 标记为管理员模式
            startActivity(intent);
            finish();
        });

        // 返回登录按钮点击事件
        backToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminChooseActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}