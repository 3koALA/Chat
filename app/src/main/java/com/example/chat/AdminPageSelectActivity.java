package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AdminPageSelectActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button userManagementButton;
    private Button modelManagementButton;
    private Button backToChooseButton;
    private Button logoutButton;

    private Long userId;
    private String username;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page_select);

        // 获取传递的用户信息
        Intent intent = getIntent();
        userId = intent.getLongExtra("userId", -1);
        username = intent.getStringExtra("username");
        role = intent.getStringExtra("role");
        String mode = intent.getStringExtra("mode");

        // 初始化视图
        initViews();

        // 设置欢迎语
        welcomeText.setText("管理员: " + username);

        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        userManagementButton = findViewById(R.id.userManagementButton);
        modelManagementButton = findViewById(R.id.modelManagementButton);
        backToChooseButton = findViewById(R.id.backToChooseButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupClickListeners() {
        // 用户管理按钮
        userManagementButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPageSelectActivity.this, UserManagementActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            startActivity(intent);
        });

        // 模型管理按钮
        modelManagementButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPageSelectActivity.this, ModelManagementActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            startActivity(intent);
        });

        // 返回选择页面按钮
        backToChooseButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPageSelectActivity.this, AdminChooseActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("role", role);
            startActivity(intent);
            finish();
        });

        // 退出登录按钮
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPageSelectActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}