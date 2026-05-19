package com.pgmanager.app.ui.tenant_portal;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pgmanager.app.R;
import com.pgmanager.app.databinding.ItemPgTenantBinding;
import com.pgmanager.app.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;

public class TenantPGAdapter extends RecyclerView.Adapter<TenantPGAdapter.ViewHolder> {

    // Real apartment / room / PG-style photos from Unsplash (direct CDN links, no API key needed)
    private static final String[] PG_IMAGES = {
        "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=600&q=80",  // cozy room
        "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=600&q=80",  // apartment interior
        "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=600&q=80",  // modern room
        "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=600&q=80",  // kitchen/living
        "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=600&q=80",  // apartment building
        "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=600&q=80",  // bedroom
        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=600&q=80",  // house exterior
        "https://images.unsplash.com/photo-1554995207-c18c203602cb?w=600&q=80",  // living room
        "https://images.unsplash.com/photo-1536376072261-38c75010e6c9?w=600&q=80",  // furnished room
        "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=600&q=80",  // bedroom interior
        "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?w=600&q=80",  // apartment room
        "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=600&q=80",  // hotel-style room
        "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&q=80",  // cozy bedroom
        "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=600&q=80",  // modern bedroom
        "https://images.unsplash.com/photo-1560185007-cde436f6a4d0?w=600&q=80",  // apartment exterior
        "https://images.unsplash.com/photo-1567767292278-a4f21aa2d36e?w=600&q=80",  // clean room
        "https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?w=600&q=80",  // furnished bedroom
        "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=600&q=80",  // hotel room
        "https://images.unsplash.com/photo-1543489822-c49534f3271f?w=600&q=80",  // shared room
        "https://images.unsplash.com/photo-1507089947368-19c1da9775ae?w=600&q=80",  // bright room
    };

    private List<TenantPortalActivity.PGWithOwner> list = new ArrayList<>();
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContact(TenantPortalActivity.PGWithOwner item);
    }

    public TenantPGAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<TenantPortalActivity.PGWithOwner> newList) {
        this.list = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPgTenantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String getImageUrl(TenantPortalActivity.PGWithOwner item) {
        // Owner-set custom photo takes priority
        String custom = item.pg.getPhotoUrl();
        if (custom != null && !custom.isEmpty()) return custom;
        // Pick from curated list using PG name hash — same PG always gets same image
        int idx = Math.abs(item.pg.getName().hashCode()) % PG_IMAGES.length;
        return PG_IMAGES[idx];
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPgTenantBinding binding;

        ViewHolder(ItemPgTenantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TenantPortalActivity.PGWithOwner item) {
            binding.tvPGName.setText(item.pg.getName());
            binding.tvAddress.setText(item.pg.getAddress() != null
                    ? item.pg.getAddress() : "Address not specified");
            binding.tvRent.setText(CurrencyUtils.formatCurrency(item.pg.getDefaultRent()) + "/mo");
            binding.tvOwnerName.setText("Owner: " + item.ownerName);
            binding.tvOwnerPhone.setText(item.ownerPhone.isEmpty()
                    ? "Contact via app" : "+91 " + item.ownerPhone);

            if (!item.ownerName.isEmpty())
                binding.tvOwnerAvatar.setText(
                        String.valueOf(item.ownerName.charAt(0)).toUpperCase());

            int vacant = Math.max(0, item.pg.getTotalBeds() - item.pg.getOccupiedBeds());
            binding.tvVacant.setText(vacant + (vacant == 1 ? " bed vacant" : " beds vacant"));
            binding.tvVacant.setBackgroundTintList(
                    itemView.getContext().getResources().getColorStateList(
                            vacant > 0 ? R.color.accent : R.color.danger));

            Glide.with(itemView.getContext())
                    .load(getImageUrl(item))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.gradient_bg)
                    .error(R.drawable.gradient_bg)
                    .centerCrop()
                    .into(binding.ivPGImage);

            if (item.ownerPhone.isEmpty()) {
                binding.btnContact.setVisibility(View.GONE);
            } else {
                binding.btnContact.setVisibility(View.VISIBLE);
                binding.btnContact.setOnClickListener(v -> listener.onContact(item));
            }

            // Map button — use exact coords if available, else text search
            String address = item.pg.getAddress();
            double lat = item.pg.getLatitude();
            double lng = item.pg.getLongitude();
            boolean hasCoords  = lat != 0 && lng != 0;
            boolean hasAddress = address != null && !address.isEmpty();
            if (hasCoords || hasAddress) {
                binding.btnViewMap.setVisibility(View.VISIBLE);
                binding.btnViewMap.setOnClickListener(v -> {
                    Uri uri = hasCoords
                            ? Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(item.pg.getName()) + ")")
                            : Uri.parse("geo:0,0?q=" + Uri.encode(address));
                    try {
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(v.getContext().getPackageManager()) != null) {
                            v.getContext().startActivity(mapIntent);
                        } else {
                            String query = hasCoords ? lat + "," + lng : Uri.encode(address);
                            v.getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://maps.google.com/?q=" + query)));
                        }
                    } catch (Exception e) {
                        String query = hasCoords ? lat + "," + lng : Uri.encode(address);
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://maps.google.com/?q=" + query)));
                    }
                });
            } else {
                binding.btnViewMap.setVisibility(View.GONE);
            }
        }
    }
}
