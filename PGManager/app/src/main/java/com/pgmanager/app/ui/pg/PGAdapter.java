package com.pgmanager.app.ui.pg;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.databinding.ItemPgBinding;
import com.pgmanager.app.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;

public class PGAdapter extends RecyclerView.Adapter<PGAdapter.PGViewHolder> {
    private List<PG> pgList = new ArrayList<>();
    private final OnPGClickListener listener;

    public interface OnPGClickListener {
        void onPGClick(PG pg);
    }

    public PGAdapter(OnPGClickListener listener) {
        this.listener = listener;
    }

    public void setPGList(List<PG> newList) {
        this.pgList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PGViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PGViewHolder(ItemPgBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PGViewHolder holder, int position) {
        holder.bind(pgList.get(position));
    }

    @Override
    public int getItemCount() { return pgList.size(); }

    class PGViewHolder extends RecyclerView.ViewHolder {
        private final ItemPgBinding binding;

        PGViewHolder(ItemPgBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PG pg) {
            binding.tvPGName.setText(pg.getName());
            binding.tvAddress.setText(pg.getAddress());
            binding.tvDefaultRent.setText(CurrencyUtils.formatCurrency(pg.getDefaultRent()));
            binding.tvRooms.setText(String.valueOf(pg.getTotalRooms()));
            binding.tvTotalBeds.setText(String.valueOf(pg.getTotalBeds()));
            int vacant = pg.getTotalBeds() - pg.getOccupiedBeds();
            binding.tvVacantBeds.setText(String.valueOf(Math.max(0, vacant)));
            int pct = pg.getTotalBeds() > 0 ? (pg.getOccupiedBeds() * 100 / pg.getTotalBeds()) : 0;
            binding.progressOccupancy.setProgress(pct);
            binding.tvOccupancyPct.setText(pct + "%");
            itemView.setOnClickListener(v -> listener.onPGClick(pg));
            binding.tvOccupancy.setOnClickListener(v -> listener.onPGClick(pg));
        }
    }
}
