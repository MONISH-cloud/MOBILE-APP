package com.pgmanager.app.ui.expense;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.Expense;
import com.pgmanager.app.databinding.ItemExpenseBinding;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private List<Expense> expenseList = new ArrayList<>();

    public void setExpenseList(List<Expense> newList) {
        this.expenseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExpenseBinding binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ExpenseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExpenseBinding binding;

        public ExpenseViewHolder(ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Expense expense) {
            binding.tvCategory.setText(expense.getCategory());
            binding.tvDescription.setText(expense.getDescription());
            binding.tvAmount.setText("-" + CurrencyUtils.formatCurrency(expense.getAmount()));
            binding.tvDate.setText(DateUtils.formatTimestamp(expense.getDate()));
            
            // Set emoji icon based on category
            if (expense.getCategory().contains("Vegetables")) binding.tvIcon.setText("🥬");
            else if (expense.getCategory().contains("Repairs")) binding.tvIcon.setText("🔧");
            else if (expense.getCategory().contains("Electricity")) binding.tvIcon.setText("⚡");
            else if (expense.getCategory().contains("Water")) binding.tvIcon.setText("💧");
            else if (expense.getCategory().contains("Cleaning")) binding.tvIcon.setText("🧹");
            else if (expense.getCategory().contains("Salary")) binding.tvIcon.setText("💰");
            else binding.tvIcon.setText("📦");
        }
    }
}
