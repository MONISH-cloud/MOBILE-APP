package com.pgmanager.app.ui.tenant_portal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.model.UserProfile;
import com.pgmanager.app.data.repository.AuthRepository;
import com.pgmanager.app.databinding.ActivityTenantPortalBinding;
import com.pgmanager.app.ui.auth.LoginActivity;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import com.pgmanager.app.util.WhatsAppHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TenantPortalActivity extends AppCompatActivity {

    private ActivityTenantPortalBinding binding;
    private TenantPGAdapter adapter;
    private AuthRepository authRepo;
    private List<PGWithOwner> allPGs = new ArrayList<>();
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTenantPortalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepo = new AuthRepository();
        currentUid = authRepo.getUid();

        loadUserName();
        setupRecyclerView();
        setupTabs();
        loadAllPGs();
        loadMyPG();
        setupSearch();

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadAllPGs();
            loadMyPG();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────

    private void setupTabs() {
        showBrowseTab();
        binding.btnTabBrowse.setOnClickListener(v -> showBrowseTab());
        binding.btnTabMyPG.setOnClickListener(v -> {
            showMyPGTab();
            loadMyPG();
        });
    }

    private void showBrowseTab() {
        binding.btnTabBrowse.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
        binding.btnTabBrowse.setTextColor(getResources().getColor(R.color.white));
        binding.btnTabMyPG.setBackgroundTintList(getResources().getColorStateList(R.color.surface_variant));
        binding.btnTabMyPG.setTextColor(getResources().getColor(R.color.text_secondary));
        binding.layoutBrowse.setVisibility(View.VISIBLE);
        binding.layoutMyPG.setVisibility(View.GONE);
    }

    private void showMyPGTab() {
        binding.btnTabMyPG.setBackgroundTintList(getResources().getColorStateList(R.color.accent));
        binding.btnTabMyPG.setTextColor(getResources().getColor(R.color.white));
        binding.btnTabBrowse.setBackgroundTintList(getResources().getColorStateList(R.color.surface_variant));
        binding.btnTabBrowse.setTextColor(getResources().getColor(R.color.text_secondary));
        binding.layoutBrowse.setVisibility(View.GONE);
        binding.layoutMyPG.setVisibility(View.VISIBLE);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private void loadUserName() {
        if (currentUid == null) return;
        authRepo.getUserProfile(currentUid).addOnSuccessListener(profile -> {
            if (profile != null && binding != null)
                binding.tvWelcome.setText("Hi, " + profile.getName() + " 👋");
        });
    }

    // ── Browse PGs ────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new TenantPGAdapter(item -> {
            String msg = "Hi, I'm interested in your PG \"" + item.pg.getName()
                    + "\" listed on PG Manager. Could you please share more details?";
            WhatsAppHelper.sendWhatsAppMessage(this, item.ownerPhone, msg);
        });
        binding.rvPGs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPGs.setAdapter(adapter);
    }

    private void loadAllPGs() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.COLLECTION_PGS)
                .addSnapshotListener((pgSnap, error) -> {
                    if (binding == null) return;
                    if (error != null || pgSnap == null) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load PGs", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    allPGs.clear();
                    if (pgSnap.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.tvPGCount.setText("0 PGs available");
                        adapter.setList(allPGs);
                        return;
                    }
                    int total = pgSnap.size();
                    int[] done = {0};
                    for (QueryDocumentSnapshot pgDoc : pgSnap) {
                        PG pg = pgDoc.toObject(PG.class);
                        if (pg.getId() == null) pg.setId(pgDoc.getId());
                        String ownerId = pg.getOwnerId();
                        if (ownerId == null) {
                            allPGs.add(new PGWithOwner(pg, "", "Owner"));
                            if (++done[0] == total) finishLoading();
                            continue;
                        }
                        db.collection("users").document(ownerId).get()
                                .addOnSuccessListener(userDoc -> {
                                    UserProfile owner = userDoc.toObject(UserProfile.class);
                                    allPGs.add(new PGWithOwner(pg,
                                            owner != null ? owner.getPhone() : "",
                                            owner != null ? owner.getName() : "Owner"));
                                    if (++done[0] == total) finishLoading();
                                })
                                .addOnFailureListener(e -> {
                                    allPGs.add(new PGWithOwner(pg, "", "Owner"));
                                    if (++done[0] == total) finishLoading();
                                });
                    }
                });
    }

    private void finishLoading() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.GONE);
        binding.tvPGCount.setText(allPGs.size() + " PGs available");
        binding.tvEmpty.setVisibility(allPGs.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setList(allPGs);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPGs(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterPGs(String query) {
        if (query.isEmpty()) { adapter.setList(allPGs); return; }
        String lower = query.toLowerCase();
        List<PGWithOwner> filtered = new ArrayList<>();
        for (PGWithOwner item : allPGs) {
            if (item.pg.getName().toLowerCase().contains(lower)
                    || (item.pg.getAddress() != null && item.pg.getAddress().toLowerCase().contains(lower)))
                filtered.add(item);
        }
        adapter.setList(filtered);
    }

    // ── My PG ─────────────────────────────────────────────────────────────────

    private void loadMyPG() {
        if (currentUid == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        authRepo.getUserProfile(currentUid).addOnSuccessListener(profile -> {
            if (profile == null) { showNoMyPG(); return; }
            db.collection(Constants.COLLECTION_TENANTS)
                    .whereEqualTo("whatsappNumber", profile.getPhone())
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) { showNoMyPG(); return; }
                        Tenant tenant = null;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            Tenant t = doc.toObject(Tenant.class);
                            if (t != null && !Constants.STATUS_MOVED_OUT.equals(t.getStatus())) {
                                tenant = t;
                                break;
                            }
                        }
                        if (tenant == null) { showNoMyPG(); return; }
                        populateMyPG(tenant, db);
                    })
                    .addOnFailureListener(e -> showNoMyPG());
        });
    }

    private void populateMyPG(Tenant tenant, FirebaseFirestore db) {
        if (binding == null) return;
        binding.layoutNoMyPG.setVisibility(View.GONE);
        binding.cardMyPGDetails.setVisibility(View.VISIBLE);

        binding.tvMyPGName.setText(tenant.getPgName());
        binding.tvMyRoom.setText("Room " + tenant.getRoomNumber() + "  •  Bed " + tenant.getBedNumber());
        binding.tvMyRent.setText(CurrencyUtils.formatCurrency(tenant.getRentAmount()) + "/mo");
        binding.tvMyJoined.setText("Joined: " + DateUtils.formatTimestamp(tenant.getJoiningDate()));
        binding.tvMyStatus.setText(tenant.getStatus().toUpperCase());
        int statusColor = Constants.STATUS_NOTICE.equals(tenant.getStatus()) ? R.color.warning : R.color.accent;
        binding.tvMyStatus.setTextColor(getResources().getColor(statusColor));

        // Map button — opens PG address in Google Maps
        db.collection(Constants.COLLECTION_PGS).document(tenant.getPgId()).get()
                .addOnSuccessListener(pgDoc -> {
                    if (!pgDoc.exists() || binding == null) return;
                    String address = pgDoc.getString("address");
                    Double lat = pgDoc.getDouble("latitude");
                    Double lng = pgDoc.getDouble("longitude");
                    boolean hasCoords = lat != null && lng != null && lat != 0 && lng != 0;
                    boolean hasAddress = address != null && !address.isEmpty();
                    if (hasCoords || hasAddress) {
                        binding.btnMyPGMap.setVisibility(android.view.View.VISIBLE);
                        binding.btnMyPGMap.setOnClickListener(v -> {
                            if (hasCoords) openMapCoords(lat, lng, tenant.getPgName());
                            else openMap(address);
                        });
                    } else {
                        binding.btnMyPGMap.setVisibility(android.view.View.GONE);
                    }
                    String ownerId = pgDoc.getString("ownerId");
                    if (ownerId == null) return;
                    db.collection("users").document(ownerId).get()
                            .addOnSuccessListener(userDoc -> {
                                UserProfile owner = userDoc.toObject(UserProfile.class);
                                if (owner == null || binding == null) return;
                                binding.tvMyOwnerName.setText("Owner: " + owner.getName());
                                binding.tvMyOwnerPhone.setText("+91 " + owner.getPhone());
                                binding.btnContactOwner.setVisibility(View.VISIBLE);
                                binding.btnContactOwner.setOnClickListener(v -> {
                                    String msg = "Hi " + owner.getName() + ", I'm "
                                            + tenant.getName() + " staying at "
                                            + tenant.getPgName() + " (Room " + tenant.getRoomNumber() + "). ";
                                    WhatsAppHelper.sendWhatsAppMessage(this, owner.getPhone(), msg);
                                });
                                binding.btnRaiseRequest.setVisibility(View.VISIBLE);
                                binding.btnRaiseRequest.setOnClickListener(v ->
                                        showRaiseRequestDialog(tenant));
                            });
                });

        // Latest rent record
        db.collection(Constants.COLLECTION_RENT_RECORDS)
                .whereEqualTo("tenantId", tenant.getId())
                .orderBy("month", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(rentSnap -> {
                    if (rentSnap.isEmpty() || binding == null) return;
                    RentRecord r = rentSnap.getDocuments().get(0).toObject(RentRecord.class);
                    if (r == null) return;
                    binding.tvMyRentStatus.setText("Rent " + r.getMonth() + ": " + r.getStatus().toUpperCase());
                    int color = Constants.STATUS_PAID.equals(r.getStatus()) ? R.color.accent
                            : Constants.STATUS_OVERDUE.equals(r.getStatus()) ? R.color.danger : R.color.warning;
                    binding.tvMyRentStatus.setTextColor(getResources().getColor(color));
                });

        // Load tenant's own requests
        loadMyRequests(tenant);
    }

    private void loadMyRequests(Tenant tenant) {
        if (binding == null) return;
        binding.cardMyRequests.setVisibility(View.VISIBLE);

        RequestAdapter reqAdapter = new RequestAdapter();
        binding.rvMyRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyRequests.setAdapter(reqAdapter);

        FirebaseFirestore.getInstance().collection("complaints")
                .whereEqualTo("tenantId", tenant.getId())
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || binding == null) return;
                    List<Complaint> requests = snap.toObjects(Complaint.class);
                    requests.sort((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    reqAdapter.setList(requests);
                    binding.tvNoRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showRaiseRequestDialog(Tenant tenant) {
        String[] categories = {
                "🔧 Plumbing", "💡 Electrical", "🧹 Cleaning",
                "❄️ AC / Fan", "🚪 Door / Lock", "💧 Water Issue", "📦 Other"
        };
        final int[] selectedCat = {0};
        final String[] selectedPriority = {"normal"};

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(56, 24, 56, 8);

        android.widget.TextView catLabel = new android.widget.TextView(this);
        catLabel.setText("Category");
        catLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        catLabel.setTextSize(12f);
        layout.addView(catLabel);

        android.widget.Spinner catSpinner = new android.widget.Spinner(this);
        catSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
        catSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { selectedCat[0] = pos; }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        layout.addView(catSpinner);

        android.widget.TextView priLabel = new android.widget.TextView(this);
        priLabel.setText("Priority");
        priLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        priLabel.setTextSize(12f);
        priLabel.setPadding(0, 16, 0, 0);
        layout.addView(priLabel);

        android.widget.Spinner priSpinner = new android.widget.Spinner(this);
        priSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Normal", "Urgent", "Low"}));
        priSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                selectedPriority[0] = pos == 1 ? "urgent" : pos == 2 ? "low" : "normal";
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        layout.addView(priSpinner);

        android.widget.TextView descLabel = new android.widget.TextView(this);
        descLabel.setText("Description");
        descLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        descLabel.setTextSize(12f);
        descLabel.setPadding(0, 16, 0, 0);
        layout.addView(descLabel);

        android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Describe the issue...");
        etDesc.setMinLines(2);
        layout.addView(etDesc);

        new AlertDialog.Builder(this)
                .setTitle("🔧 Raise Maintenance Request")
                .setMessage("PG: " + tenant.getPgName() + "  •  Room " + tenant.getRoomNumber())
                .setView(layout)
                .setPositiveButton("Submit", (d, w) -> {
                    String desc = etDesc.getText().toString().trim();
                    if (desc.isEmpty()) {
                        Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String id = db.collection("complaints").document().getId();
                    Complaint c = new Complaint();
                    c.setId(id);
                    c.setTenantId(tenant.getId());
                    c.setTenantName(tenant.getName());
                    c.setPgId(tenant.getPgId());
                    c.setUnit("Room " + tenant.getRoomNumber() + " / Bed " + tenant.getBedNumber());
                    c.setDescription(categories[selectedCat[0]] + " — " + desc);
                    c.setPriority(selectedPriority[0]);
                    c.setStatus("pending");
                    c.setCreatedAt(Timestamp.now());
                    c.setUpdatedAt(Timestamp.now());
                    db.collection("complaints").document(id).set(c)
                            .addOnSuccessListener(a -> Toast.makeText(this,
                                    "✅ Request submitted! Owner will see it shortly.",
                                    Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openMapCoords(double lat, double lng, String label) {
        try {
            Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + lat + "," + lng)));
            }
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=" + lat + "," + lng)));
        }
    }

    private void openMap(String address) {
        try {
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + Uri.encode(address))));
            }
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=" + Uri.encode(address))));
        }
    }

    private void showNoMyPG() {
        if (binding == null) return;
        binding.layoutNoMyPG.setVisibility(View.VISIBLE);
        binding.cardMyPGDetails.setVisibility(View.GONE);
        binding.cardMyRequests.setVisibility(View.GONE);
    }

    // ── Request Adapter ───────────────────────────────────────────────────────

    static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {
        private List<Complaint> list = new ArrayList<>();

        void setList(List<Complaint> l) { this.list = l; notifyDataSetChanged(); }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tenant_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Complaint c = list.get(pos);
            String desc = c.getDescription() != null ? c.getDescription() : "";
            int dashIdx = desc.indexOf(" — ");
            if (dashIdx > 0) {
                h.tvCategory.setText(desc.substring(0, dashIdx));
                h.tvDesc.setText(desc.substring(dashIdx + 3));
            } else {
                h.tvCategory.setText("Request");
                h.tvDesc.setText(desc);
            }

            String statusLabel;
            int statusColor;
            switch (c.getStatus() != null ? c.getStatus() : "pending") {
                case "resolved":   statusLabel = "✅ Resolved";   statusColor = R.color.accent;   break;
                case "in_progress": statusLabel = "🔧 In Progress"; statusColor = R.color.warning; break;
                default:           statusLabel = "⏳ Pending";    statusColor = R.color.danger;
            }
            h.tvStatus.setText(statusLabel);
            h.tvStatus.setTextColor(h.itemView.getContext().getResources().getColor(statusColor));
            h.tvStatus.setBackgroundTintList(
                    h.itemView.getContext().getResources().getColorStateList(R.color.transparent));

            if (c.getCreatedAt() != null) {
                h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                        .format(new Date(c.getCreatedAt().getSeconds() * 1000)));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCategory, tvDesc, tvStatus, tvDate;
            VH(View v) {
                super(v);
                tvCategory = v.findViewById(R.id.tvReqCategory);
                tvDesc = v.findViewById(R.id.tvReqDescription);
                tvStatus = v.findViewById(R.id.tvReqStatus);
                tvDate = v.findViewById(R.id.tvReqDate);
            }
        }
    }

    // ── Data holder ───────────────────────────────────────────────────────────

    public static class PGWithOwner {
        public final PG pg;
        public final String ownerPhone;
        public final String ownerName;

        public PGWithOwner(PG pg, String ownerPhone, String ownerName) {
            this.pg = pg;
            this.ownerPhone = ownerPhone;
            this.ownerName = ownerName;
        }
    }
}
