package com.pgmanager.app.ui.complaints;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.R;
import com.pgmanager.app.data.model.Complaint;
import com.pgmanager.app.databinding.ActivityComplaintsBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ComplaintsActivity extends AppCompatActivity {
    private ActivityComplaintsBinding binding;
    private FirebaseFirestore db;
    private ComplaintAdapter adapter;
    private List<Complaint> allComplaints = new ArrayList<>();
    private List<String> ownerPgIds = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        adapter = new ComplaintAdapter();
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        // Owner can still manually add a complaint on behalf of tenant
        binding.fabAdd.setOnClickListener(v -> showAddDialog());

        binding.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == binding.chipPending.getId()) currentFilter = "pending";
            else if (id == binding.chipInProgress.getId()) currentFilter = "in_progress";
            else if (id == binding.chipResolved.getId()) currentFilter = "resolved";
            else currentFilter = "all";
            applyFilter();
        });

        loadOwnerPGsThenComplaints();
    }

    private void loadOwnerPGsThenComplaints() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    ownerPgIds.clear();
                    for (QueryDocumentSnapshot doc : snap) ownerPgIds.add(doc.getId());
                    if (ownerPgIds.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    loadComplaints();
                });
    }

    private void loadComplaints() {
        List<String> pgBatch = ownerPgIds.subList(0, Math.min(ownerPgIds.size(), 10));
        db.collection("complaints")
                .whereIn("pgId", pgBatch)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || binding == null) return;
                    allComplaints = snap.toObjects(Complaint.class);
                    applyFilter();
                    updateBadge();
                });
    }

    private void applyFilter() {
        List<Complaint> filtered = new ArrayList<>();
        for (Complaint c : allComplaints) {
            if ("all".equals(currentFilter) || currentFilter.equals(c.getStatus()))
                filtered.add(c);
        }
        adapter.setList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateBadge() {
        int pending = 0;
        for (Complaint c : allComplaints)
            if ("pending".equals(c.getStatus())) pending++;
        binding.tvBadge.setText(pending > 0 ? String.valueOf(pending) : "");
        binding.tvBadge.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
    }

    private void showAddDialog() {
        if (ownerPgIds.isEmpty()) {
            Toast.makeText(this, "No PGs found", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(56, 32, 56, 16);

        android.widget.EditText etTenant = new android.widget.EditText(this);
        etTenant.setHint("Tenant Name");
        layout.addView(etTenant);

        android.widget.EditText etUnit = new android.widget.EditText(this);
        etUnit.setHint("Room / Unit");
        layout.addView(etUnit);

        android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Description");
        etDesc.setMinLines(2);
        layout.addView(etDesc);

        new AlertDialog.Builder(this)
                .setTitle("Add Complaint")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String tenant = etTenant.getText().toString().trim();
                    String unit = etUnit.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (tenant.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Complaint c = new Complaint();
                    c.setId(db.collection("complaints").document().getId());
                    c.setTenantName(tenant);
                    c.setUnit(unit);
                    c.setDescription(desc);
                    c.setPriority("normal");
                    c.setStatus("pending");
                    c.setPgId(ownerPgIds.get(0));
                    c.setCreatedAt(Timestamp.now());
                    c.setUpdatedAt(Timestamp.now());
                    db.collection("complaints").document(c.getId()).set(c)
                            .addOnSuccessListener(a ->
                                    Toast.makeText(this, "Complaint added", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────
    class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.VH> {
        private List<Complaint> list = new ArrayList<>();

        void setList(List<Complaint> l) {
            this.list = l;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_complaint, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Complaint c = list.get(pos);
            h.tvTenantName.setText(c.getTenantName());
            h.tvUnit.setText(c.getUnit() != null ? "🚪 " + c.getUnit() : "");
            h.tvDescription.setText(c.getDescription());

            // Priority badge
            boolean urgent = "urgent".equals(c.getPriority());
            h.tvPriority.setText(urgent ? "🔴 URGENT" : c.getPriority().toUpperCase(Locale.ENGLISH));
            h.tvPriority.setBackgroundTintList(getResources().getColorStateList(
                    urgent ? R.color.danger : R.color.surface_variant));
            h.tvPriority.setTextColor(getResources().getColor(
                    urgent ? R.color.white : R.color.text_secondary));

            // Status
            String statusLabel;
            int statusColor;
            switch (c.getStatus()) {
                case "resolved":
                    statusLabel = "✅ Resolved";
                    statusColor = R.color.accent;
                    break;
                case "in_progress":
                    statusLabel = "🔧 In Progress";
                    statusColor = R.color.warning;
                    break;
                default:
                    statusLabel = "⏳ Pending";
                    statusColor = R.color.danger;
            }
            h.tvStatus.setText(statusLabel);
            h.tvStatus.setTextColor(getResources().getColor(statusColor));

            // Advance button
            if ("resolved".equals(c.getStatus())) {
                h.btnAdvance.setVisibility(View.GONE);
            } else {
                h.btnAdvance.setVisibility(View.VISIBLE);
                String nextStatus = "pending".equals(c.getStatus()) ? "in_progress" : "resolved";
                String btnLabel = "pending".equals(c.getStatus()) ? "▶ Start" : "✅ Resolve";
                ((com.google.android.material.button.MaterialButton) h.btnAdvance).setText(btnLabel);
                int btnColor = "pending".equals(c.getStatus()) ? R.color.primary : R.color.accent;
                ((com.google.android.material.button.MaterialButton) h.btnAdvance)
                        .setBackgroundTintList(getResources().getColorStateList(btnColor));
                h.btnAdvance.setOnClickListener(v ->
                        db.collection("complaints").document(c.getId())
                                .update("status", nextStatus, "updatedAt", Timestamp.now())
                                .addOnSuccessListener(a ->
                                        Toast.makeText(ComplaintsActivity.this,
                                                "Status updated", Toast.LENGTH_SHORT).show()));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTenantName, tvUnit, tvDescription, tvPriority, tvStatus;
            View btnAdvance;

            VH(View v) {
                super(v);
                tvTenantName = v.findViewById(R.id.tvTenantName);
                tvUnit = v.findViewById(R.id.tvUnit);
                tvDescription = v.findViewById(R.id.tvDescription);
                tvPriority = v.findViewById(R.id.tvPriority);
                tvStatus = v.findViewById(R.id.tvStatus);
                btnAdvance = v.findViewById(R.id.btnAdvance);
            }
        }
    }
}
