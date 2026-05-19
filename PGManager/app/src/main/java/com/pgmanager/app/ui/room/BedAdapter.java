package com.pgmanager.app.ui.room;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.databinding.ItemBedBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;
import java.util.List;

public class BedAdapter extends RecyclerView.Adapter<BedAdapter.BedViewHolder> {
    private List<Bed> bedList = new ArrayList<>();
    private final OnBedClickListener clickListener;
    private final OnBedClickListener longClickListener;

    public interface OnBedClickListener {
        void onBedClick(Bed bed);
    }

    public BedAdapter(OnBedClickListener clickListener, OnBedClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setBedList(List<Bed> newList) {
        this.bedList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BedViewHolder(ItemBedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BedViewHolder holder, int position) {
        holder.bind(bedList.get(position));
    }

    @Override
    public int getItemCount() { return bedList.size(); }

    class BedViewHolder extends RecyclerView.ViewHolder {
        private final ItemBedBinding binding;

        BedViewHolder(ItemBedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Bed bed) {
            boolean isOccupied = Constants.BED_OCCUPIED.equals(bed.getStatus());
            binding.tvBedNumber.setText(bed.getBedNumber());
            binding.tvBedIcon.setText(isOccupied ? "👤" : "🛏️");
            binding.tvBedStatus.setText(isOccupied
                    ? (bed.getTenantName() != null ? bed.getTenantName() : "Occupied")
                    : "Vacant");
            int color = isOccupied
                    ? itemView.getContext().getResources().getColor(com.pgmanager.app.R.color.status_occupied)
                    : itemView.getContext().getResources().getColor(com.pgmanager.app.R.color.status_vacant);
            binding.tvBedStatus.setTextColor(color);
            itemView.setOnClickListener(v -> clickListener.onBedClick(bed));
            itemView.setOnLongClickListener(v -> { longClickListener.onBedClick(bed); return true; });
        }
    }
}
