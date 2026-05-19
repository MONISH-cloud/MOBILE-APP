package com.pgmanager.app.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.R;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.repository.AuthRepository;
import com.pgmanager.app.databinding.FragmentDashboardBinding;
import com.pgmanager.app.ui.rent.RentAdapter;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import com.pgmanager.app.util.WhatsAppHelper;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private AuthRepository authRepo;
    private RentAdapter rentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authRepo = new AuthRepository();

        loadProfile();
        setupDueList();
        loadStats();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadStats();
            binding.swipeRefresh.setRefreshing(false);
        });

        binding.qaProperties.setOnClickListener(v -> navigateTo(R.id.nav_pgs));
        binding.qaRent.setOnClickListener(v -> navigateTo(R.id.nav_rent));
        binding.qaExpenses.setOnClickListener(v -> navigateTo(R.id.nav_settings));
    }









    private void navigateTo(int navId) {
        try {
            if (getActivity() != null)
                androidx.navigation.Navigation
                        .findNavController(getActivity(), R.id.nav_host_fragment)
                        .navigate(navId);
        } catch (Exception ignored) {}
    }

    private void loadProfile() {
        String uid = authRepo.getUid();
        if (uid == null) return;
        authRepo.getUserProfile(uid).addOnSuccessListener(profile -> {
            if (profile != null && binding != null)
                binding.tvUserName.setText(profile.getName());
        });
    }

    private void setupDueList() {
        rentAdapter = new RentAdapter(new RentAdapter.OnRentActionListener() {
            @Override
            public void onMarkPaid(RentRecord record) {
                FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_RENT_RECORDS)
                        .document(record.getId())
                        .update("status", Constants.STATUS_PAID, "paidDate", Timestamp.now())
                        .addOnSuccessListener(a -> loadStats());
            }
            @Override
            public void onRemind(RentRecord record) {
                String msg = WhatsAppHelper.getRentReminderMessage(
                        record.getTenantName(), record.getAmount(), record.getPgName());
                WhatsAppHelper.sendWhatsAppMessage(getContext(), record.getWhatsappNumber(), msg);
            }
        });
        binding.rvDueToday.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDueToday.setAdapter(rentAdapter);
    }

    private void loadStats() {
        if (binding == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: get owner's PG IDs first, then scope all queries
        db.collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(pgSnap -> {
                    if (binding == null) return;

                    List<String> pgIds = new ArrayList<>();
                    int totalBeds = 0, occupiedBeds = 0;
                    for (QueryDocumentSnapshot doc : pgSnap) {
                        pgIds.add(doc.getId());
                        Long tb = doc.getLong("totalBeds");
                        Long ob = doc.getLong("occupiedBeds");
                        totalBeds += tb != null ? tb.intValue() : 0;
                        occupiedBeds += ob != null ? ob.intValue() : 0;
                    }

                    int pct = totalBeds > 0 ? (occupiedBeds * 100 / totalBeds) : 0;
                    binding.tvOccupancy.setText(pct + "%");
                    binding.progressOccupancy.setProgress(pct);

                    if (pgIds.isEmpty()) {
                        binding.tvTotalIncome.setText(CurrencyUtils.formatCurrency(0));
                        binding.tvTotalExpense.setText(CurrencyUtils.formatCurrency(0));
                        binding.tvOverdueCount.setText("0");
                        binding.tvPendingCount.setText("0");
                        binding.tvNoDue.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<String> pgBatch = pgIds.subList(0, Math.min(pgIds.size(), 10));
                    String currentMonth = DateUtils.getCurrentMonth();

                    // Rent records scoped to owner's PGs
                    db.collection(Constants.COLLECTION_RENT_RECORDS)
                            .whereIn("pgId", pgBatch)
                            .whereEqualTo("month", currentMonth)
                            .get()
                            .addOnSuccessListener(snap -> {
                                if (binding == null) return;
                                double totalIncome = 0, totalOverdue = 0;
                                int overdueCount = 0, pendingCount = 0;
                                List<RentRecord> pendingOverdue = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : snap) {
                                    RentRecord r = doc.toObject(RentRecord.class);
                                    if (Constants.STATUS_PAID.equals(r.getStatus())) {
                                        totalIncome += r.getAmount();
                                    } else if (Constants.STATUS_OVERDUE.equals(r.getStatus())) {
                                        totalOverdue += r.getAmount();
                                        overdueCount++;
                                        pendingOverdue.add(r);
                                    } else if (Constants.STATUS_PENDING.equals(r.getStatus())) {
                                        pendingCount++;
                                        pendingOverdue.add(r);
                                    }
                                }

                                binding.tvTotalIncome.setText(CurrencyUtils.formatCurrency(totalIncome));
                                binding.tvTotalExpense.setText(CurrencyUtils.formatCurrency(totalOverdue));
                                binding.tvOverdueCount.setText(String.valueOf(overdueCount));
                                binding.tvPendingCount.setText(String.valueOf(pendingCount));
                                rentAdapter.setRentList(pendingOverdue);
                                binding.tvNoDue.setVisibility(pendingOverdue.isEmpty() ? View.VISIBLE : View.GONE);
                            });

                    // Tenants scoped to owner's PGs
                    db.collection(Constants.COLLECTION_TENANTS)
                            .whereIn("pgId", pgBatch)
                            .get()
                            .addOnSuccessListener(snap -> {
                                if (binding == null) return;
                                int notices = 0;
                                for (QueryDocumentSnapshot doc : snap) {
                                    Tenant t = doc.toObject(Tenant.class);
                                    if (Constants.STATUS_NOTICE.equals(t.getStatus())) notices++;
                                }
                                binding.tvNotices.setText(String.valueOf(notices));
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
