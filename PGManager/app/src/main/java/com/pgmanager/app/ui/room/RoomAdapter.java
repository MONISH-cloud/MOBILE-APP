package com.pgmanager.app.ui.room;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.data.model.Room;
import com.pgmanager.app.databinding.ItemRoomBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    private List<Room> roomList = new ArrayList<>();
    private final String pgId;
    private final BedAdapter.OnBedClickListener bedClickListener;
    private final OnRoomActionListener roomActionListener;

    public interface OnRoomActionListener {
        void onDeleteRoom(Room room);
        void onAddBed(Room room);
    }

    public RoomAdapter(String pgId, BedAdapter.OnBedClickListener bedClickListener, OnRoomActionListener roomActionListener) {
        this.pgId = pgId;
        this.bedClickListener = bedClickListener;
        this.roomActionListener = roomActionListener;
    }

    public void setRoomList(List<Room> newList) {
        this.roomList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RoomViewHolder(ItemRoomBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        holder.bind(roomList.get(position));
    }

    @Override
    public int getItemCount() { return roomList.size(); }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoomBinding binding;

        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Room room) {
            binding.tvRoomNumber.setText("Room " + room.getRoomNumber());
            binding.tvFloor.setText("Floor " + room.getFloor());
            binding.tvOccupancy.setText(room.getOccupiedBeds() + "/" + room.getTotalBeds() + " occupied");

            binding.btnAddBed.setOnClickListener(v -> roomActionListener.onAddBed(room));
            binding.btnDeleteRoom.setOnClickListener(v -> {
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Room " + room.getRoomNumber())
                        .setMessage("This will delete all beds in this room. Occupied beds cannot be deleted.")
                        .setPositiveButton("Delete", (d, w) -> roomActionListener.onDeleteRoom(room))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            BedAdapter bedAdapter = new BedAdapter(bedClickListener, (bed) -> {
                // Long press = delete bed
                if (Constants.BED_OCCUPIED.equals(bed.getStatus())) {
                    Toast.makeText(itemView.getContext(), "Cannot delete occupied bed", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Bed " + bed.getBedNumber())
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (d, w) -> {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection(Constants.COLLECTION_PGS).document(pgId)
                                    .collection(Constants.COLLECTION_ROOMS).document(room.getId())
                                    .collection(Constants.COLLECTION_BEDS).document(bed.getId())
                                    .delete()
                                    .addOnSuccessListener(a -> {
                                        db.collection(Constants.COLLECTION_PGS).document(pgId)
                                                .collection(Constants.COLLECTION_ROOMS).document(room.getId())
                                                .update("totalBeds", com.google.firebase.firestore.FieldValue.increment(-1));
                                        db.collection(Constants.COLLECTION_PGS).document(pgId)
                                                .update("totalBeds", com.google.firebase.firestore.FieldValue.increment(-1));
                                        Toast.makeText(itemView.getContext(), "Bed deleted", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            binding.rvBeds.setLayoutManager(new GridLayoutManager(itemView.getContext(), 3));
            binding.rvBeds.setAdapter(bedAdapter);
            binding.rvBeds.setHasFixedSize(false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Constants.COLLECTION_PGS).document(pgId)
                    .collection(Constants.COLLECTION_ROOMS).document(room.getId())
                    .collection(Constants.COLLECTION_BEDS)
                    .orderBy("bedNumber")
                    .addSnapshotListener((value, error) -> {
                        if (value == null) return;
                        List<Bed> beds = value.toObjects(Bed.class);
                        List<Bed> occupiedWithTenant = new ArrayList<>();
                        for (Bed bed : beds) {
                            if (Constants.BED_OCCUPIED.equals(bed.getStatus()) && bed.getTenantId() != null)
                                occupiedWithTenant.add(bed);
                        }
                        if (occupiedWithTenant.isEmpty()) { bedAdapter.setBedList(beds); return; }
                        int[] loaded = {0};
                        int total = occupiedWithTenant.size();
                        for (Bed bed : occupiedWithTenant) {
                            db.collection(Constants.COLLECTION_TENANTS).document(bed.getTenantId()).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) bed.setTenantName(doc.getString("name"));
                                        if (++loaded[0] == total) bedAdapter.setBedList(beds);
                                    })
                                    .addOnFailureListener(e -> { if (++loaded[0] == total) bedAdapter.setBedList(beds); });
                        }
                    });
        }
    }
}
