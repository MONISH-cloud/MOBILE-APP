package com.pgmanager.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.data.repository.AuthRepository;
import com.pgmanager.app.databinding.FragmentSettingsBinding;
import com.pgmanager.app.ui.auth.LoginActivity;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import com.pgmanager.app.util.SeedDataUtil;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private AuthRepository authRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authRepo = new AuthRepository();

        loadProfile();
        loadMonthlyExpenses();

        binding.tvRefreshExpenses.setOnClickListener(v -> loadMonthlyExpenses());

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnInvite.setOnClickListener(v -> shareInvite());
        binding.btnWhatsAppHub.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.pgmanager.app.ui.whatsapp.WhatsAppHubActivity.class)));
        binding.btnComplaints.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.pgmanager.app.ui.complaints.ComplaintsActivity.class)));
        binding.btnNotifications.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.pgmanager.app.ui.notifications.NotificationsActivity.class)));
        binding.btnSeedData.setOnClickListener(v -> {
            binding.btnSeedData.setEnabled(false);
            binding.btnSeedData.setText("Loading...");
            SeedDataUtil.seedDemoData(new SeedDataUtil.OnSeedComplete() {
                @Override
                public void onComplete() {
                    if (binding == null) return;
                    binding.btnSeedData.setText("✅ Demo Data Loaded!");
                    Toast.makeText(getContext(), "Demo PG with 10 rooms and 12 tenants added!", Toast.LENGTH_LONG).show();
                    loadMonthlyExpenses();
                }
                @Override
                public void onError(String error) {
                    if (binding == null) return;
                    binding.btnSeedData.setEnabled(true);
                    binding.btnSeedData.setText("🏠 Load Demo Data");
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadProfile() {
        String uid = authRepo.getUid();
        if (uid == null) return;
        authRepo.getUserProfile(uid).addOnSuccessListener(profile -> {
            if (profile != null && binding != null) {
                binding.tvName.setText(profile.getName());
                binding.tvPhone.setText(profile.getPhone());
                binding.tvRole.setText(profile.getRole().toUpperCase(Locale.ENGLISH));
                binding.tvAvatar.setText(profile.getName().substring(0, 1).toUpperCase(Locale.ENGLISH));
                binding.tvPlan.setText(profile.getSubscriptionPlan().toUpperCase(Locale.ENGLISH) + " PLAN");
            }
        });
    }

    private void loadMonthlyExpenses() {
        if (binding == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        binding.tvTotalMonthlyExpense.setText("Loading...");
        binding.llExpenseBreakdown.removeAllViews();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentMonth = DateUtils.getCurrentMonth();

        // Get all PGs owned by user
        db.collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(pgSnap -> {
                    if (binding == null) return;
                    if (pgSnap.isEmpty()) {
                        binding.tvTotalMonthlyExpense.setText(CurrencyUtils.formatCurrency(0));
                        addBreakdownRow("No PGs found", "₹0");
                        return;
                    }

                    int pgCount = pgSnap.size();
                    AtomicInteger done = new AtomicInteger(0);
                    AtomicReference<Double> grandTotal = new AtomicReference<>(0.0);
                    Map<String, Double> pgExpenses = new HashMap<>();

                    for (QueryDocumentSnapshot pgDoc : pgSnap) {
                        String pgId = pgDoc.getId();
                        String pgName = pgDoc.getString("name") != null ? pgDoc.getString("name") : "PG";

                        db.collection(Constants.COLLECTION_PGS).document(pgId)
                                .collection(Constants.COLLECTION_EXPENSES)
                                .get()
                                .addOnSuccessListener(expSnap -> {
                                    double pgTotal = 0;
                                    for (QueryDocumentSnapshot expDoc : expSnap) {
                                        Double amount = expDoc.getDouble("amount");
                                        if (amount != null) pgTotal += amount;
                                    }
                                    pgExpenses.put(pgName, pgTotal);
                                    grandTotal.set(grandTotal.get() + pgTotal);

                                    if (done.incrementAndGet() == pgCount && binding != null) {
                                        binding.tvTotalMonthlyExpense.setText(CurrencyUtils.formatCurrency(grandTotal.get()));
                                        binding.llExpenseBreakdown.removeAllViews();
                                        for (Map.Entry<String, Double> entry : pgExpenses.entrySet()) {
                                            addBreakdownRow(entry.getKey(), CurrencyUtils.formatCurrency(entry.getValue()));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (done.incrementAndGet() == pgCount && binding != null) {
                                        binding.tvTotalMonthlyExpense.setText(CurrencyUtils.formatCurrency(grandTotal.get()));
                                    }
                                });
                    }
                });
    }

    private void addBreakdownRow(String pgName, String amount) {
        if (binding == null || getContext() == null) return;
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 0);
        row.setLayoutParams(params);

        TextView tvName = new TextView(getContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText("• " + pgName);
        tvName.setTextColor(getResources().getColor(com.pgmanager.app.R.color.text_secondary));
        tvName.setTextSize(13f);

        TextView tvAmount = new TextView(getContext());
        tvAmount.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvAmount.setText(amount);
        tvAmount.setTextColor(getResources().getColor(com.pgmanager.app.R.color.status_overdue));
        tvAmount.setTextSize(13f);

        row.addView(tvName);
        row.addView(tvAmount);
        binding.llExpenseBreakdown.addView(row);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    authRepo.logout();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    getActivity().finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareInvite() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my PG Manager as a manager! Download the app: [Link]");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share via"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
