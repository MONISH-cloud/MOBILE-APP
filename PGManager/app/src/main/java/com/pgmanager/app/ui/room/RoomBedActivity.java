package com.pgmanager.app.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.data.model.Room;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.databinding.ActivityRoomBedBinding;
import com.pgmanager.app.databinding.DialogAddRoomBinding;
import com.pgmanager.app.ui.tenant.AddEditTenantActivity;
import com.pgmanager.app.ui.tenant.TenantDetailActivity;
import com.pgmanager.app.util.Constants;
import java.util.List;

public class RoomBedActivity extends AppCompatActivity {
    private ActivityRoomBedBinding binding;
    private PGRepository pgRepo;
    private String pgId;
    private RoomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoomBedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pgRepo = new PGRepository();
        pgId = getIntent().getStringExtra(Constants.EXTRA_PG_ID);

        setupRecyclerView();
        loadRooms();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    private void setupRecyclerView() {
        adapter = new RoomAdapter(pgId,
            // Tap bed
            bed -> {
                if (Constants.BED_OCCUPIED.equals(bed.getStatus())) {
                    Intent intent = new Intent(this, TenantDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_TENANT_ID, bed.getTenantId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, AddEditTenantActivity.class);
                    intent.putExtra(Constants.EXTRA_PG_ID, pgId);
                    startActivity(intent);
                }
            },
            // Room actions
            new RoomAdapter.OnRoomActionListener() {
                @Override
                public void onDeleteRoom(Room room) {
                    deleteRoom(room);
                }
                @Override
                public void onAddBed(Room room) {
                    showAddBedDialog(room);
                }
            }
        );
        binding.rvRooms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRooms.setAdapter(adapter);
    }

    private void loadRooms() {
        pgRepo.getRoomCollection(pgId).orderBy("roomNumber").addSnapshotListener((value, error) -> {
            if (value != null) adapter.setRoomList(value.toObjects(Room.class));
        });
    }

    private void showAddRoomDialog() {
        DialogAddRoomBinding dialogBinding = DialogAddRoomBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();
        dialogBinding.btnAddRoom.setOnClickListener(v -> {
            String roomNum = dialogBinding.etRoomNumber.getText().toString().trim();
            String floorStr = dialogBinding.etFloor.getText().toString().trim();
            String bedsStr = dialogBinding.etNumBeds.getText().toString().trim();
            if (roomNum.isEmpty() || bedsStr.isEmpty()) {
                Toast.makeText(this, "Fill room number and beds", Toast.LENGTH_SHORT).show();
                return;
            }
            int floor = floorStr.isEmpty() ? 1 : Integer.parseInt(floorStr);
            int beds = Integer.parseInt(bedsStr);
            if (beds < 1 || beds > 20) {
                Toast.makeText(this, "Beds must be between 1 and 20", Toast.LENGTH_SHORT).show();
                return;
            }
            addRoomWithBeds(roomNum, floor, beds);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showAddBedDialog(Room room) {
        // Count existing beds to get next bed number
        pgRepo.getBedCollection(pgId, room.getId()).get().addOnSuccessListener(snap -> {
            int nextNum = snap.size() + 1;
            String defaultBedNum = room.getRoomNumber() + "-" + nextNum;

            android.widget.EditText et = new android.widget.EditText(this);
            et.setText(defaultBedNum);
            et.setHint("Bed number");
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setPadding(56, 32, 56, 16);
            layout.addView(et);

            new AlertDialog.Builder(this)
                    .setTitle("Add Bed to Room " + room.getRoomNumber())
                    .setView(layout)
                    .setPositiveButton("Add", (d, w) -> {
                        String bedNum = et.getText().toString().trim();
                        if (bedNum.isEmpty()) return;
                        String bedId = pgRepo.getBedCollection(pgId, room.getId()).document().getId();
                        Bed bed = new Bed();
                        bed.setId(bedId);
                        bed.setBedNumber(bedNum);
                        bed.setStatus(Constants.BED_VACANT);
                        pgRepo.getBedCollection(pgId, room.getId()).document(bedId).set(bed)
                                .addOnSuccessListener(a -> {
                                    // Update room and PG totals
                                    pgRepo.getRoomCollection(pgId).document(room.getId())
                                            .update("totalBeds", com.google.firebase.firestore.FieldValue.increment(1));
                                    pgRepo.getPGCollection().document(pgId)
                                            .update("totalBeds", com.google.firebase.firestore.FieldValue.increment(1));
                                    Toast.makeText(this, "Bed " + bedNum + " added", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void addRoomWithBeds(String roomNum, int floor, int bedsCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        String roomId = pgRepo.getRoomCollection(pgId).document().getId();
        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber(roomNum);
        room.setFloor(floor);
        room.setTotalBeds(bedsCount);
        room.setOccupiedBeds(0);
        batch.set(pgRepo.getRoomCollection(pgId).document(roomId), room);

        for (int i = 1; i <= bedsCount; i++) {
            String bedId = pgRepo.getBedCollection(pgId, roomId).document().getId();
            Bed bed = new Bed();
            bed.setId(bedId);
            bed.setBedNumber(roomNum + "-" + i);
            bed.setStatus(Constants.BED_VACANT);
            batch.set(pgRepo.getBedCollection(pgId, roomId).document(bedId), bed);
        }

        batch.commit().addOnSuccessListener(a -> {
            pgRepo.getPGCollection().document(pgId).update(
                    "totalRooms", com.google.firebase.firestore.FieldValue.increment(1),
                    "totalBeds", com.google.firebase.firestore.FieldValue.increment(bedsCount));
            Toast.makeText(this, "Room " + roomNum + " added", Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteRoom(Room room) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Check no occupied beds
        pgRepo.getBedCollection(pgId, room.getId())
                .whereEqualTo("status", Constants.BED_OCCUPIED).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Toast.makeText(this, "Cannot delete — " + snap.size() + " bed(s) are occupied", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Delete all beds then room
                    pgRepo.getBedCollection(pgId, room.getId()).get().addOnSuccessListener(bedSnap -> {
                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot doc : bedSnap) {
                            batch.delete(doc.getReference());
                        }
                        batch.delete(pgRepo.getRoomCollection(pgId).document(room.getId()));
                        batch.commit().addOnSuccessListener(a -> {
                            pgRepo.getPGCollection().document(pgId).update(
                                    "totalRooms", com.google.firebase.firestore.FieldValue.increment(-1),
                                    "totalBeds", com.google.firebase.firestore.FieldValue.increment(-room.getTotalBeds()));
                            Toast.makeText(this, "Room " + room.getRoomNumber() + " deleted", Toast.LENGTH_SHORT).show();
                        });
                    });
                });
    }
}
