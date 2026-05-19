package com.pgmanager.app.ui.tenant;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.databinding.ItemTenantBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TenantAdapter extends RecyclerView.Adapter<TenantAdapter.TenantViewHolder> {
    private List<Tenant> tenantList = new ArrayList<>();
    private final OnTenantClickListener listener;

    public interface OnTenantClickListener {
        void onTenantClick(Tenant tenant);
    }

    public TenantAdapter(OnTenantClickListener listener) {
        this.listener = listener;
    }

    public void setTenantList(List<Tenant> newList) {
        this.tenantList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TenantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTenantBinding binding = ItemTenantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TenantViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TenantViewHolder holder, int position) {
        Tenant tenant = tenantList.get(position);
        holder.bind(tenant);
    }

    @Override
    public int getItemCount() {
        return tenantList.size();
    }

    class TenantViewHolder extends RecyclerView.ViewHolder {
        private final ItemTenantBinding binding;

        public TenantViewHolder(ItemTenantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Tenant tenant) {
            binding.tvName.setText(tenant.getName());
            binding.tvPGRoom.setText(tenant.getPgName() + " • Room " + tenant.getRoomNumber());
            binding.tvRent.setText(CurrencyUtils.formatCurrency(tenant.getRentAmount()));
            binding.tvAvatar.setText(tenant.getName().substring(0, 1).toUpperCase());
            
            binding.tvStatus.setText(tenant.getStatus().toUpperCase(Locale.ENGLISH));
            if (Constants.STATUS_NOTICE.equals(tenant.getStatus())) {
                binding.tvStatus.setBackgroundResource(com.pgmanager.app.R.drawable.status_badge); // Amber/Notice style
            }
            
            itemView.setOnClickListener(v -> listener.onTenantClick(tenant));
        }
    }
}
