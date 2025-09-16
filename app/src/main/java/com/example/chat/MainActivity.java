package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.chat.beans.User;
import com.example.chat.fragments.ChatFragment;
import com.example.chat.fragments.ModelSelectionFragment;
import com.example.chat.fragments.UserProfileFragment;
import com.example.chat.fragments.VoiceChatFragment;
import com.example.chat.managers.UserManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // 保存 Fragment 实例
    private Fragment modelFragment;
    private Fragment chatFragment;
    private Fragment voiceFragment;
    private Fragment currentFragment;
    private Fragment userProfileFragment;

    // 用户信息
    private long userId = -1;
    private String username;
    private String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 从Intent获取用户信息
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getLongExtra("userId", -1);
            username = intent.getStringExtra("username");
            nickname = intent.getStringExtra("nickname");
            System.out.println(userId);
            // 如果是从登录跳转过来，保存用户信息到UserManager
            if (userId != -1 && username != null) {
                User user = new User();
                user.setId(userId);
                user.setUsername(username);
                user.setNickname(nickname);
                UserManager.getInstance().setCurrentUser(user);
            }
        }

        // 如果Intent中没有用户信息，但从UserManager中可以获取
        if (userId == -1) {
            User currentUser = UserManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getId();
                username = currentUser.getUsername();
                nickname = currentUser.getNickname();
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            modelFragment = new ModelSelectionFragment();
            chatFragment = new ChatFragment();
            voiceFragment = new VoiceChatFragment();
            userProfileFragment = new UserProfileFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, modelFragment, "model")
                    .add(R.id.fragment_container, chatFragment, "chat")
                    .hide(chatFragment)
                    .add(R.id.fragment_container, voiceFragment, "voice")
                    .hide(voiceFragment)
                    .add(R.id.fragment_container, userProfileFragment, "profile")
                    .hide(userProfileFragment)
                    .commit();

            currentFragment = modelFragment;
        } else {
            // Fragment恢复由系统自动完成
            modelFragment = getSupportFragmentManager().findFragmentByTag("model");
            chatFragment = getSupportFragmentManager().findFragmentByTag("chat");
            voiceFragment = getSupportFragmentManager().findFragmentByTag("voice");
            userProfileFragment = getSupportFragmentManager().findFragmentByTag("profile");

            // 恢复用户信息
            userId = savedInstanceState.getLong("userId", -1);
            username = savedInstanceState.getString("username");
            nickname = savedInstanceState.getString("nickname");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("userId", userId);
        outState.putString("username", username);
        if (nickname != null) {
            outState.putString("nickname", nickname);
        }
    }

    // 提供方法让Fragment获取用户信息
    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    private NavigationBarView.OnItemSelectedListener navListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment targetFragment = null;
                    if (item.getItemId() == R.id.nav_models) {
                        targetFragment = modelFragment;
                    } else if (item.getItemId() == R.id.nav_chat) {
                        targetFragment = chatFragment;
                    } else if (item.getItemId() == R.id.nav_voice) {
                        targetFragment = voiceFragment;
                    } else if (item.getItemId() == R.id.nav_profile) {
                        targetFragment = userProfileFragment;
                    }

                    if (targetFragment != null && targetFragment != currentFragment) {
                        getSupportFragmentManager().beginTransaction()
                                .hide(currentFragment)
                                .show(targetFragment)
                                .commit();
                        currentFragment = targetFragment;
                    }
                    return true;
                }
            };
}