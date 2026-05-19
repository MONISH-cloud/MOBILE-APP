package com.pgmanager.app.ui.reports;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.databinding.FragmentReportsBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReportsFragment extends Fragment {
    private FragmentReportsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadRealData();
    }

    private void loadRealData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentMonth = DateUtils.getCurrentMonth();

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
                        totalBeds += tb != null ? tb : 0;
                        occupiedBeds += ob != null ? ob : 0;
                    }

                    final int finalTotalBeds = totalBeds;
                    final int finalOccupied = occupiedBeds;

                    if (pgIds.isEmpty()) {
                        setupCharts(0, 0, 0, 0);
                        return;
                    }

                    List<String> batch = pgIds.subList(0, Math.min(pgIds.size(), 10));

                    // Load rent records for current month
                    db.collection(Constants.COLLECTION_RENT_RECORDS)
                            .whereIn("pgId", batch)
                            .whereEqualTo("month", currentMonth)
                            .get()
                            .addOnSuccessListener(rentSnap -> {
                                if (binding == null) return;
                                double income = 0, overdue = 0;
                                int paid = 0;
                                for (QueryDocumentSnapshot doc : rentSnap) {
                                    Double amt = doc.getDouble("amount");
                                    String status = doc.getString("status");
                                    if (Constants.STATUS_PAID.equals(status)) {
                                        income += amt != null ? amt : 0;
                                        paid++;
                                    } else if (Constants.STATUS_OVERDUE.equals(status)) {
                                        overdue += amt != null ? amt : 0;
                                    }
                                }

                                final double finalIncome = income;
                                final double finalOverdue = overdue;
                                final int finalPaid = paid;
                                final int finalTotal = rentSnap.size();

                                // Load expenses
                                AtomicReference<Double> totalExpense = new AtomicReference<>(0.0);
                                AtomicInteger expDone = new AtomicInteger(0);
                                int pgCount = pgIds.size();

                                for (String pgId : pgIds) {
                                    db.collection(Constants.COLLECTION_PGS).document(pgId)
                                            .collection(Constants.COLLECTION_EXPENSES).get()
                                            .addOnSuccessListener(expSnap -> {
                                                for (QueryDocumentSnapshot e : expSnap) {
                                                    Double a = e.getDouble("amount");
                                                    if (a != null) totalExpense.set(totalExpense.get() + a);
                                                }
                                                if (expDone.incrementAndGet() == pgCount && binding != null) {
                                                    int collectionPct = finalTotal > 0 ? (finalPaid * 100 / finalTotal) : 0;
                                                    setupCharts(finalIncome, totalExpense.get(), finalOccupied, finalTotalBeds);
                                                    updateStats(finalIncome, totalExpense.get(), finalOverdue, collectionPct);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                if (expDone.incrementAndGet() == pgCount && binding != null) {
                                                    int collectionPct = finalTotal > 0 ? (finalPaid * 100 / finalTotal) : 0;
                                                    setupCharts(finalIncome, totalExpense.get(), finalOccupied, finalTotalBeds);
                                                    updateStats(finalIncome, totalExpense.get(), finalOverdue, collectionPct);
                                                }
                                            });
                                }
                            });
                });
    }

    private void setupCharts(double income, double expense, int occupied, int totalBeds) {
        if (binding == null) return;

        // Bar chart — income vs expenses
        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(1, (float) income));
        barEntries.add(new BarEntry(2, (float) expense));
        BarDataSet barSet = new BarDataSet(barEntries, "Income vs Expenses");
        barSet.setColors(new int[]{Color.parseColor("#06D6A0"), Color.parseColor("#EF476F")});
        binding.chartProfit.setData(new BarData(barSet));
        binding.chartProfit.animateY(800);
        binding.chartProfit.getDescription().setEnabled(false);
        binding.chartProfit.getLegend().setEnabled(false);
        binding.chartProfit.invalidate();

        // Pie chart — occupancy
        int vacant = Math.max(0, totalBeds - occupied);
        List<PieEntry> pieEntries = new ArrayList<>();
        if (occupied > 0) pieEntries.add(new PieEntry(occupied, "Occupied"));
        if (vacant > 0)   pieEntries.add(new PieEntry(vacant, "Vacant"));
        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(1, "No beds"));
        }
        PieDataSet pieSet = new PieDataSet(pieEntries, "");
        pieSet.setColors(new int[]{Color.parseColor("#8338EC"), Color.parseColor("#E2E8F0")});
        pieSet.setValueTextColor(Color.WHITE);
        binding.chartOccupancy.setData(new PieData(pieSet));
        binding.chartOccupancy.animateX(800);
        binding.chartOccupancy.getDescription().setEnabled(false);
        binding.chartOccupancy.getLegend().setEnabled(false);
        binding.chartOccupancy.invalidate();
    }

    private void updateStats(double income, double expense, double overdue, int collectionPct) {
        if (binding == null) return;
        binding.tvTotalIncome.setText(CurrencyUtils.formatCurrency(income));
        binding.tvTotalExpenseReport.setText(CurrencyUtils.formatCurrency(expense));
        binding.tvNetProfit.setText(CurrencyUtils.formatCurrency(income - expense));
        binding.progressCollection.setProgress(collectionPct);
        binding.tvCollectionRate.setText(collectionPct + "% collected");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
