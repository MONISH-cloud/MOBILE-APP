package com.pgmanager.app.ui.expense;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.pgmanager.app.data.model.Expense;
import com.pgmanager.app.data.repository.ExpenseRepository;
import com.pgmanager.app.databinding.FragmentExpenseListBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import java.util.List;

public class ExpenseListFragment extends Fragment {
    private FragmentExpenseListBinding binding;
    private ExpenseRepository expenseRepo;
    private String pgId;
    private ExpenseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExpenseListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseRepo = new ExpenseRepository();
        pgId = getActivity() != null ? getActivity().getIntent().getStringExtra(Constants.EXTRA_PG_ID) : null;

        setupRecyclerView();
        if (pgId != null) loadExpenses();

        binding.btnBack.setOnClickListener(v -> { if (getActivity() != null) getActivity().finish(); });
        binding.fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddExpenseActivity.class);
            intent.putExtra(Constants.EXTRA_PG_ID, pgId);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void loadExpenses() {
        expenseRepo.getMonthlyExpenses(pgId).addSnapshotListener((value, error) -> {
            if (binding == null) return;
            if (value != null) {
                List<Expense> expenses = value.toObjects(Expense.class);
                adapter.setExpenseList(expenses);
                
                double total = 0;
                for (Expense e : expenses) total += e.getAmount();
                binding.tvTotalExpense.setText("Total: " + CurrencyUtils.formatCurrency(total));
                binding.tvEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
