package com.pgmanager.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.firebase.auth.FirebaseAuth;
import com.pgmanager.app.R;
import com.pgmanager.app.databinding.ActivityMainBinding;
import com.pgmanager.app.ui.auth.LoginActivity;
import com.pgmanager.app.ui.chat.ChatbotActivity;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }

        binding.fabChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatbotActivity.class)));

        // Show tooltip only on dashboard tab, auto-hide after 3s
        showTooltip();
        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_dashboard) {
                showTooltip();
            } else {
                binding.tvHelpTooltip.setVisibility(View.GONE);
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    private void showTooltip() {
        binding.tvHelpTooltip.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        binding.tvHelpTooltip.startAnimation(fadeIn);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(600);
            fadeOut.setFillAfter(true);
            binding.tvHelpTooltip.startAnimation(fadeOut);
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    binding.tvHelpTooltip.setVisibility(View.GONE), 600);
        }, 3000);
    }
}
