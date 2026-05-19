package com.pgmanager.app.ui.rent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.data.repository.RentRepository;
import com.pgmanager.app.databinding.FragmentRentDashboardBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.WhatsAppHelper;
import java.util.ArrayList;
import java.util.List;

public class RentDashboardFragment extends Fragment {
    private FragmentRentDashboardBinding binding;
    private RentRepository rentRepo;
    private RentAdapter adapter;
    private List<String> ownerPgIds = new ArrayList<>();
    private String activeFilter = null; // null = all

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rentRepo = new RentRepository();
        setupRecyclerView();
        setupFilters();
        loadOwnerPGsThenRent();
    }

    private void setupRecyclerView() {
        adapter = new RentAdapter(new RentAdapter.OnRentActionListener() {
            @Override
            public void onMarkPaid(RentRecord record) {
                rentRepo.getRentCollection().document(record.getId()).update(
                        "status", Constants.STATUS_PAID,
                        "paidDate", Timestamp.now()
                ).addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Payment recorded", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onRemind(RentRecord record) {
                String message = WhatsAppHelper.getRentReminderMessage(
                        record.getTenantName(), record.getAmount(), record.getPgName());
                WhatsAppHelper.sendWhatsAppMessage(getContext(), record.getWhatsappNumber(), message);
            }
        });
        binding.rvRent.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRent.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                activeFilter = null;
            } else {
                int id = checkedIds.get(0);
                if (id == binding.chipPending.getId()) activeFilter = Constants.STATUS_PENDING;
                else if (id == binding.chipPaid.getId()) activeFilter = Constants.STATUS_PAID;
                else if (id == binding.chipOverdue.getId()) activeFilter = Constants.STATUS_OVERDUE;
                else activeFilter = null;
            }
            loadRentForOwner();
        });
    }

    private void loadOwnerPGsThenRent() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    ownerPgIds.clear();
                    for (QueryDocumentSnapshot doc : snap) ownerPgIds.add(doc.getId());
                    loadRentForOwner();
                });
    }

    private void loadRentForOwner() {
        if (ownerPgIds.isEmpty()) {
            if (binding != null) {
                adapter.setRentList(new ArrayList<>());
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        // Firestore whereIn supports up to 10 values
        List<String> batch = ownerPgIds.subList(0, Math.min(ownerPgIds.size(), 10));
        Query query = rentRepo.getRentCollection()
                .whereIn("pgId", batch)
                .orderBy("dueDate", Query.Direction.DESCENDING);

        if (activeFilter != null) {
            query = rentRepo.getRentCollection()
                    .whereIn("pgId", batch)
                    .whereEqualTo("status", activeFilter)
                    .orderBy("dueDate", Query.Direction.DESCENDING);
        }

        query.addSnapshotListener((value, error) -> {
            if (binding == null) return;
            if (value != null) {
                List<RentRecord> records = value.toObjects(RentRecord.class);
                adapter.setRentList(records);
                binding.tvEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
