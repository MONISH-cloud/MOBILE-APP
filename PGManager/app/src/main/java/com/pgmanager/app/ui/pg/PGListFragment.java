package com.pgmanager.app.ui.pg;

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
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.databinding.FragmentPgListBinding;
import com.pgmanager.app.util.Constants;
import java.util.List;

public class PGListFragment extends Fragment {
    private FragmentPgListBinding binding;
    private PGRepository pgRepo;
    private PGAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPgListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pgRepo = new PGRepository();
        
        setupRecyclerView();
        loadPGs();

        binding.fabAddPG.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), AddEditPGActivity.class));
        });
    }

    private void setupRecyclerView() {
        adapter = new PGAdapter(pg -> {
            Intent intent = new Intent(getContext(), PGDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PG_ID, pg.getId());
            startActivity(intent);
        });
        binding.rvPGs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPGs.setAdapter(adapter);
    }

    private void loadPGs() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getUid();
        pgRepo.getMyPGs(userId).addSnapshotListener((value, error) -> {
            binding.progressBar.setVisibility(View.GONE);
            if (value != null) {
                List<PG> pgs = value.toObjects(PG.class);
                adapter.setPGList(pgs);
                binding.tvPGCount.setText(pgs.size() + " properties");
                binding.tvEmpty.setVisibility(pgs.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
