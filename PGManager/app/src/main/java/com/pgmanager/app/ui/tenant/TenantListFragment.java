package com.pgmanager.app.ui.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.repository.TenantRepository;
import com.pgmanager.app.databinding.FragmentTenantListBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;
import java.util.List;

public class TenantListFragment extends Fragment {
    private FragmentTenantListBinding binding;
    private TenantAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTenantListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TenantAdapter(tenant -> {
            Intent intent = new Intent(getContext(), TenantDetailActivity.class);
            intent.putExtra(Constants.EXTRA_TENANT_ID, tenant.getId());
            startActivity(intent);
        });
        binding.rvTenants.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTenants.setAdapter(adapter);

        binding.fabAddTenant.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddEditTenantActivity.class)));

        loadTenants();
    }

    private void loadTenants() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // First get all PGs owned by this user
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(pgSnap -> {
                    if (pgSnap.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Collect all pgIds
                    List<String> pgIds = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : pgSnap)
                        pgIds.add(doc.getId());

                    // Query tenants for these PGs (Firestore whereIn supports up to 10)
                    List<String> batch = pgIds.subList(0, Math.min(pgIds.size(), 10));

                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection(Constants.COLLECTION_TENANTS)
                            .whereIn("pgId", batch)
                            .addSnapshotListener((value, error) -> {
                                if (binding == null) return;
                                binding.progressBar.setVisibility(View.GONE);
                                if (value != null) {
                                    List<Tenant> tenants = new ArrayList<>();
                                    for (Tenant t : value.toObjects(Tenant.class)) {
                                        if (!Constants.STATUS_MOVED_OUT.equals(t.getStatus()))
                                            tenants.add(t);
                                    }
                                    adapter.setTenantList(tenants);
                                    binding.tvTenantCount.setText(tenants.size() + " active tenants");
                                    binding.tvEmpty.setVisibility(tenants.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            });
                })
                .addOnFailureListener(e -> binding.progressBar.setVisibility(View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
