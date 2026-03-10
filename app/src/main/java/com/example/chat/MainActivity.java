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
import com.example.chat.fragments.ModelConfigFragment;
import com.example.chat.managers.UserManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // 保存 Fragment 实例
    private Fragment modelFragment;
    private Fragment chatFragment;
    private Fragment configFragment;
    private Fragment currentFragment;
    private Fragment userProfileFragment;

    // 用户信息
    private long userId = -1;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String role;

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
            avatarUrl = intent.getStringExtra("avatarUrl");
            role = intent.getStringExtra("role");
            System.out.println(userId);
            // 如果是从登录跳转过来，保存用户信息到UserManager
            if (userId != -1 && username != null) {
                User user = new User();
                user.setId(userId);
                user.setUsername(username);
                user.setNickname(nickname);
                user.setAvatarUrl(avatarUrl);
                user.setRole(role);
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
                avatarUrl = currentUser.getAvatarUrl();
                role = currentUser.getRole();
            }
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            modelFragment = new ModelSelectionFragment();
            chatFragment = new ChatFragment();
            configFragment = new ModelConfigFragment();
            userProfileFragment = new UserProfileFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, modelFragment, "model")
                    .add(R.id.fragment_container, chatFragment, "chat")
                    .hide(chatFragment)
                    .add(R.id.fragment_container, configFragment, "config")
                    .hide(configFragment)
                    .add(R.id.fragment_container, userProfileFragment, "profile")
                    .hide(userProfileFragment)
                    .commit();

            currentFragment = modelFragment;
            // 如果启动指定了 tab，则切换到对应 tab
            String startTab = getIntent().getStringExtra("start_tab");
            if (startTab != null) {
                if (startTab.equals("chat")) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_chat);
                } else if (startTab.equals("voice") || startTab.equals("config")) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_config);
                } else if (startTab.equals("profile")) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                }
            }
            // 如果通过 Intent 指定了 modelId 或 modelName（例如从模型详情页跳转），传递给 configFragment
            long passedModelId = getIntent().getLongExtra("modelId", -1);
            String passedModelName = getIntent().getStringExtra("modelName");
            if (passedModelId > 0 || (passedModelName != null && !passedModelName.isEmpty())) {
                try {
                    if (configFragment instanceof com.example.chat.fragments.ModelConfigFragment) {
                        ((com.example.chat.fragments.ModelConfigFragment) configFragment).updateForModel(passedModelId, passedModelName);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            // Fragment恢复由系统自动完成
            modelFragment = getSupportFragmentManager().findFragmentByTag("model");
            chatFragment = getSupportFragmentManager().findFragmentByTag("chat");
            configFragment = getSupportFragmentManager().findFragmentByTag("config");
            userProfileFragment = getSupportFragmentManager().findFragmentByTag("profile");

            // 恢复用户信息
            userId = savedInstanceState.getLong("userId", -1);
            username = savedInstanceState.getString("username");
            nickname = savedInstanceState.getString("nickname");
        }

        // 如果恢复分支也携带了启动参数（可能通过 Intent），也尝试更新 fragment
        long passedModelId2 = getIntent().getLongExtra("modelId", -1);
        String passedModelName2 = getIntent().getStringExtra("modelName");
        if ((passedModelId2 > 0 || (passedModelName2 != null && !passedModelName2.isEmpty())) && configFragment != null) {
            try {
                if (configFragment instanceof com.example.chat.fragments.ModelConfigFragment) {
                    ((com.example.chat.fragments.ModelConfigFragment) configFragment).updateForModel(passedModelId2, passedModelName2);
                }
            } catch (Exception ignored) {}
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
                    } else if (item.getItemId() == R.id.nav_config) {
                        targetFragment = configFragment;
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