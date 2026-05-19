package com.pgmanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.pgmanager.app.data.model.UserProfile;
import com.pgmanager.app.data.repository.AuthRepository;
import com.pgmanager.app.databinding.ActivityLoginBinding;
import com.pgmanager.app.ui.main.MainActivity;
import com.pgmanager.app.ui.tenant_portal.TenantPortalActivity;
import com.pgmanager.app.util.Constants;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private AuthRepository authRepo;
    private boolean isLoginMode = true;
    private String selectedRole = Constants.ROLE_OWNER; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        authRepo = new AuthRepository();

        if (mAuth.getCurrentUser() != null) {
            routeByRole(mAuth.getCurrentUser().getUid());
            return;
        }

        updateUI();
        setupRoleToggle();

        binding.btnSendOTP.setOnClickListener(v -> handleAuth());
        binding.tvToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }

    private void setupRoleToggle() {
        binding.btnRoleOwner.setOnClickListener(v -> {
            selectedRole = Constants.ROLE_OWNER;
            binding.btnRoleOwner.setBackgroundTintList(getResources().getColorStateList(com.pgmanager.app.R.color.primary));
            binding.btnRoleOwner.setTextColor(getResources().getColor(com.pgmanager.app.R.color.white));
            binding.btnRoleTenant.setBackgroundTintList(getResources().getColorStateList(com.pgmanager.app.R.color.surface_variant));
            binding.btnRoleTenant.setTextColor(getResources().getColor(com.pgmanager.app.R.color.text_secondary));
            updateUI();
        });
        binding.btnRoleTenant.setOnClickListener(v -> {
            selectedRole = Constants.ROLE_TENANT;
            binding.btnRoleTenant.setBackgroundTintList(getResources().getColorStateList(com.pgmanager.app.R.color.accent));
            binding.btnRoleTenant.setTextColor(getResources().getColor(com.pgmanager.app.R.color.white));
            binding.btnRoleOwner.setBackgroundTintList(getResources().getColorStateList(com.pgmanager.app.R.color.surface_variant));
            binding.btnRoleOwner.setTextColor(getResources().getColor(com.pgmanager.app.R.color.text_secondary));
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            binding.btnSendOTP.setText("Login");
            binding.tvToggleMode.setText("Don't have an account? Register");
            binding.tilName.setVisibility(View.GONE);
        } else {
            binding.btnSendOTP.setText(selectedRole.equals(Constants.ROLE_TENANT) ? "Register as Tenant" : "Register as Owner");
            binding.tvToggleMode.setText("Already have an account? Login");
            binding.tilName.setVisibility(View.VISIBLE);
        }
    }

    private void handleAuth() {
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (phone.isEmpty() || phone.length() < 10) {
            binding.tilPhone.setError("Enter valid 10-digit phone number");
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        String email = phone + "@pgmanager.com";
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSendOTP.setEnabled(false);

        if (isLoginMode) {
            login(email, password);
        } else {
            String name = binding.etName.getText().toString().trim();
            if (name.isEmpty()) {
                binding.tilName.setError("Enter your name");
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSendOTP.setEnabled(true);
                return;
            }
            register(email, password, phone, name);
        }
    }

    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSendOTP.setEnabled(true);
            if (task.isSuccessful()) {
                routeByRole(mAuth.getCurrentUser().getUid());
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void register(String email, String password, String phone, String name) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                UserProfile profile = new UserProfile(uid, name, phone, selectedRole);
                authRepo.saveUserProfile(profile).addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    navigateToPortal(selectedRole);
                    finish();
                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSendOTP.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSendOTP.setEnabled(true);
                String err = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void routeByRole(String uid) {
        authRepo.getUserProfile(uid).addOnSuccessListener(profile -> {
            if (profile != null) {
                navigateToPortal(profile.getRole());
            } else {
                navigateToPortal(selectedRole);
            }
            finish();
        }).addOnFailureListener(e -> {
            // Don't guess — show login again so user can retry
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSendOTP.setEnabled(true);
            Toast.makeText(this, "Could not load profile. Check connection.", Toast.LENGTH_LONG).show();
        });
    }

    private void navigateToPortal(String role) {
        if (Constants.ROLE_TENANT.equals(role)) {
            startActivity(new Intent(this, TenantPortalActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
