package com.pgmanager.app.ui.rent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.databinding.ItemRentBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RentAdapter extends RecyclerView.Adapter<RentAdapter.RentViewHolder> {
    private List<RentRecord> rentList = new ArrayList<>();
    private final OnRentActionListener listener;

    public interface OnRentActionListener {
        void onMarkPaid(RentRecord record);
        void onRemind(RentRecord record);
    }

    public RentAdapter(OnRentActionListener listener) {
        this.listener = listener;
    }

    public void setRentList(List<RentRecord> newList) {
        this.rentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRentBinding binding = ItemRentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RentViewHolder holder, int position) {
        RentRecord record = rentList.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return rentList.size();
    }

    class RentViewHolder extends RecyclerView.ViewHolder {
        private final ItemRentBinding binding;

        public RentViewHolder(ItemRentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RentRecord record) {
            binding.tvAvatar.setText(record.getTenantName().substring(0, 1).toUpperCase(Locale.ENGLISH));
            binding.tvTenantName.setText(record.getTenantName());
            binding.tvPGName.setText(record.getPgName());
            binding.tvAmount.setText(CurrencyUtils.formatCurrency(record.getAmount()));
            binding.tvDueDate.setText("Due: " + DateUtils.formatTimestamp(record.getDueDate()));
            binding.tvStatus.setText(record.getStatus().toUpperCase(Locale.ENGLISH));

            if (Constants.STATUS_PAID.equals(record.getStatus())) {
                binding.tvStatus.setTextColor(itemView.getContext().getResources().getColor(com.pgmanager.app.R.color.status_paid));
                binding.layoutActions.setVisibility(View.GONE);
            } else if (Constants.STATUS_OVERDUE.equals(record.getStatus())) {
                binding.tvStatus.setTextColor(itemView.getContext().getResources().getColor(com.pgmanager.app.R.color.danger));
                binding.layoutActions.setVisibility(View.VISIBLE);
            } else {
                binding.tvStatus.setTextColor(itemView.getContext().getResources().getColor(com.pgmanager.app.R.color.warning));
                binding.layoutActions.setVisibility(View.VISIBLE);
            }

            binding.btnMarkPaid.setOnClickListener(v -> listener.onMarkPaid(record));
            binding.btnRemind.setOnClickListener(v -> listener.onRemind(record));
        }
    }
}
