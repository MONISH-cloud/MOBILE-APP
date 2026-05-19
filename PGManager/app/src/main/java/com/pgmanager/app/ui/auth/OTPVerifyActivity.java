package com.pgmanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.pgmanager.app.data.model.UserProfile;
import com.pgmanager.app.data.repository.AuthRepository;
import com.pgmanager.app.databinding.ActivityOtpVerifyBinding;
import com.pgmanager.app.ui.main.MainActivity;
import com.pgmanager.app.util.Constants;

public class OTPVerifyActivity extends AppCompatActivity {
    private ActivityOtpVerifyBinding binding;
    private String mVerificationId;
    private AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepo = new AuthRepository();
        mVerificationId = getIntent().getStringExtra("verificationId");
        String phone = getIntent().getStringExtra("phone");
        binding.tvPhoneDisplay.setText("+91 " + phone);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnVerify.setOnClickListener(v -> {
            String code = binding.etOtp.getText().toString().trim();
            if (code.length() < 6) {
                Toast.makeText(this, "Enter 6-digit code", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });
    }

    private void verifyCode(String code) {
        binding.progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                checkUserProfile();
            } else {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OTPVerifyActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserProfile() {
        String uid = authRepo.getUid();
        authRepo.getUserProfile(uid).addOnSuccessListener(profile -> {
            if (profile == null) {
                // First time login - create profile
                UserProfile newProfile = new UserProfile(uid, "Owner", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(), Constants.ROLE_OWNER);
                authRepo.saveUserProfile(newProfile).addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(OTPVerifyActivity.this, MainActivity.class));
                    finishAffinity();
                });
            } else {
                startActivity(new Intent(OTPVerifyActivity.this, MainActivity.class));
                finishAffinity();
            }
        });
    }
}
