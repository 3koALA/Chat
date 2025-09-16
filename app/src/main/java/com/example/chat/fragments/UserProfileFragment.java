package com.example.chat.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chat.LoginActivity;
import com.example.chat.MainActivity;
import com.example.chat.R;
import com.example.chat.beans.User;
import com.example.chat.managers.UserManager;

public class UserProfileFragment extends Fragment {

    private TextView userNicknameTextView;
    private Button actionButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        // 初始化视图
        userNicknameTextView = view.findViewById(R.id.userNicknameTextView);
        actionButton = view.findViewById(R.id.actionButton);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次Fragment可见时更新UI状态
        updateUI();
    }

    private void updateUI() {
        User user = UserManager.getInstance().getCurrentUser();

        if (user != null && user.getId() != -1) {
            // 已登录状态
            String displayName = user.getNickname() != null && !user.getNickname().isEmpty()
                    ? user.getNickname()
                    : user.getUsername();
            userNicknameTextView.setText(displayName);
            actionButton.setText("退出登录");
            actionButton.setOnClickListener(v -> logout());
        } else {
            // 未登录状态（游客）
            userNicknameTextView.setText("未登录");
            actionButton.setText("去登录");
            actionButton.setOnClickListener(v -> navigateToLogin());
        }
    }

    private void logout() {
        // 清除用户信息
        UserManager.getInstance().logout();

        // 显示退出成功消息
        Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();

        // 刷新UI状态
        updateUI();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }
}