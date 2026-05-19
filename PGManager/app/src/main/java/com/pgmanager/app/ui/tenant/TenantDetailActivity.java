package com.pgmanager.app.ui.tenant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.data.model.Room;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.data.repository.RentRepository;
import com.pgmanager.app.data.repository.TenantRepository;
import com.pgmanager.app.databinding.ActivityTenantDetailBinding;
import com.pgmanager.app.ui.rent.RentAdapter;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import com.pgmanager.app.util.WhatsAppHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TenantDetailActivity extends AppCompatActivity {
    private ActivityTenantDetailBinding binding;
    private TenantRepository tenantRepo;
    private RentRepository rentRepository;
    private PGRepository pgRepo;
    private String tenantId;
    private Tenant currentTenant;
    private RentAdapter rentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTenantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tenantRepo = new TenantRepository();
        rentRepository = new RentRepository();
        pgRepo = new PGRepository();
        tenantId = getIntent().getStringExtra(Constants.EXTRA_TENANT_ID);

        loadTenantData();
        setupRentHistory();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnWhatsApp.setOnClickListener(v -> {
            if (currentTenant != null)
                WhatsAppHelper.sendWhatsAppMessage(this, currentTenant.getWhatsappNumber(), "Hi " + currentTenant.getName() + ",");
        });
        binding.btnEdit.setOnClickListener(v -> showEditDialog());
        binding.btnChangeRoom.setOnClickListener(v -> {
            if (currentTenant == null) {
                Toast.makeText(this, "Loading tenant data...", Toast.LENGTH_SHORT).show();
                return;
            }
            loadRoomsAndShowPicker();
        });
        binding.btnNotice.setOnClickListener(v -> startNoticePeriod());
        binding.btnCompleteMoveOut.setOnClickListener(v -> completeMoveOut());
        binding.btnDeleteTenant.setOnClickListener(v -> showDeleteTenantDialog());
    }

    private void loadTenantData() {
        tenantRepo.getTenantCollection().document(tenantId).addSnapshotListener((snapshot, error) -> {
            if (snapshot != null && snapshot.exists()) {
                currentTenant = snapshot.toObject(Tenant.class);
                updateUI();
            }
        });
    }

    private void updateUI() {
        binding.tvName.setText(currentTenant.getName());
        binding.tvAvatar.setText(currentTenant.getName().substring(0, 1).toUpperCase(Locale.ENGLISH));
        binding.tvPhone.setText("+91 " + currentTenant.getWhatsappNumber());
        binding.tvPGName.setText(currentTenant.getPgName());
        binding.tvRoom.setText("Room " + currentTenant.getRoomNumber());
        binding.tvBed.setText("Bed " + currentTenant.getBedNumber());
        binding.tvRent.setText(CurrencyUtils.formatCurrency(currentTenant.getRentAmount()));
        binding.tvJoined.setText(DateUtils.formatTimestamp(currentTenant.getJoiningDate()));
        binding.tvStatus.setText(currentTenant.getStatus().toUpperCase(Locale.ENGLISH));

        if (Constants.STATUS_NOTICE.equals(currentTenant.getStatus())) {
            binding.cardNotice.setVisibility(View.VISIBLE);
            binding.btnNotice.setVisibility(View.GONE);
            if (currentTenant.getExpectedMoveOutDate() != null) {
                long daysLeft = DateUtils.getDaysBetween(Calendar.getInstance().getTime(),
                        currentTenant.getExpectedMoveOutDate().toDate());
                binding.tvCountdown.setText(Math.max(0, daysLeft) + " days");
                binding.tvMoveOutDate.setText("Expected: " + DateUtils.formatTimestamp(currentTenant.getExpectedMoveOutDate()));
            }
        }
    }

    private void showEditDialog() {
        if (currentTenant == null) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 32, 56, 16);

        EditText etName = new EditText(this);
        etName.setHint("Full Name");
        etName.setText(currentTenant.getName());
        etName.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("WhatsApp Number (10 digits)");
        etPhone.setText(currentTenant.getWhatsappNumber());
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        EditText etRent = new EditText(this);
        etRent.setHint("Monthly Rent (₹)");
        etRent.setText(String.valueOf((int) currentTenant.getRentAmount()));
        etRent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etRent);

        EditText etAdvance = new EditText(this);
        etAdvance.setHint("Advance Amount (₹)");
        etAdvance.setText(String.valueOf((int) currentTenant.getAdvanceAmount()));
        etAdvance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAdvance);

        new AlertDialog.Builder(this)
                .setTitle("Edit Tenant")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String rentStr = etRent.getText().toString().trim();
                    String advStr = etAdvance.getText().toString().trim();
                    if (name.isEmpty() || phone.isEmpty() || rentStr.isEmpty()) {
                        Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    tenantRepo.getTenantCollection().document(tenantId).update(
                            "name", name,
                            "whatsappNumber", phone,
                            "rentAmount", Double.parseDouble(rentStr),
                            "advanceAmount", advStr.isEmpty() ? 0 : Double.parseDouble(advStr)
                    ).addOnSuccessListener(a -> Toast.makeText(this, "✅ Updated!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Step 1: Load rooms and show room picker
    private void loadRoomsAndShowPicker() {
        pgRepo.getRoomCollection(currentTenant.getPgId()).get()
                .addOnSuccessListener(snap -> {
                    List<Room> rooms = snap.toObjects(Room.class);
                    if (rooms.isEmpty()) {
                        Toast.makeText(this, "No rooms found in this PG", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] roomItems = new String[rooms.size()];
                    for (int i = 0; i < rooms.size(); i++) {
                        Room r = rooms.get(i);
                        roomItems[i] = "Room " + r.getRoomNumber() + "  (Floor " + r.getFloor() + ")  —  "
                                + r.getOccupiedBeds() + "/" + r.getTotalBeds() + " occupied";
                    }
                    final int[] selectedRoomIdx = {-1};
                    new AlertDialog.Builder(this)
                            .setTitle("Step 1 — Select New Room\n(Current: Room " + currentTenant.getRoomNumber() + ")")
                            .setSingleChoiceItems(roomItems, -1, (d, which) -> selectedRoomIdx[0] = which)
                            .setPositiveButton("Next →", (d, w) -> {
                                if (selectedRoomIdx[0] == -1) {
                                    Toast.makeText(this, "Please select a room", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Room chosenRoom = rooms.get(selectedRoomIdx[0]);
                                loadBedsAndShowPicker(chosenRoom);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load rooms: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Step 2: Load vacant beds for chosen room and show bed picker
    private void loadBedsAndShowPicker(Room chosenRoom) {
        pgRepo.getBedCollection(currentTenant.getPgId(), chosenRoom.getId())
                .whereEqualTo("status", Constants.BED_VACANT)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Bed> beds = snap.toObjects(Bed.class);
                    if (beds.isEmpty()) {
                        Toast.makeText(this, "No vacant beds in Room " + chosenRoom.getRoomNumber(), Toast.LENGTH_LONG).show();
                        // Go back to room picker
                        loadRoomsAndShowPicker();
                        return;
                    }
                    String[] bedItems = new String[beds.size()];
                    for (int i = 0; i < beds.size(); i++) {
                        bedItems[i] = "Bed " + beds.get(i).getBedNumber() + "  (Vacant)";
                    }
                    final int[] selectedBedIdx = {-1};
                    new AlertDialog.Builder(this)
                            .setTitle("Step 2 — Select Bed in Room " + chosenRoom.getRoomNumber())
                            .setSingleChoiceItems(bedItems, -1, (d, which) -> selectedBedIdx[0] = which)
                            .setPositiveButton("Transfer", (d, w) -> {
                                if (selectedBedIdx[0] == -1) {
                                    Toast.makeText(this, "Please select a bed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Bed chosenBed = beds.get(selectedBedIdx[0]);
                                confirmAndTransfer(chosenRoom, chosenBed);
                            })
                            .setNegativeButton("← Back", (d, w) -> loadRoomsAndShowPicker())
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load beds: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Step 3: Confirm and execute transfer
    private void confirmAndTransfer(Room newRoom, Bed newBed) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Transfer")
                .setMessage("Move " + currentTenant.getName() + " from:\n"
                        + "Room " + currentTenant.getRoomNumber() + " / Bed " + currentTenant.getBedNumber()
                        + "\n\nTo:\nRoom " + newRoom.getRoomNumber() + " / Bed " + newBed.getBedNumber())
                .setPositiveButton("Confirm", (d, w) -> executeTransfer(newRoom, newBed))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeTransfer(Room newRoom, Bed newBed) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // Free old bed
        batch.update(pgRepo.getBedCollection(currentTenant.getPgId(), currentTenant.getRoomId())
                .document(currentTenant.getBedId()), "status", Constants.BED_VACANT, "tenantId", null);

        // Decrement old room occupancy
        batch.update(pgRepo.getRoomCollection(currentTenant.getPgId()).document(currentTenant.getRoomId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(-1));

        // Occupy new bed
        batch.update(pgRepo.getBedCollection(currentTenant.getPgId(), newRoom.getId())
                .document(newBed.getId()), "status", Constants.BED_OCCUPIED, "tenantId", tenantId);

        // Increment new room occupancy
        batch.update(pgRepo.getRoomCollection(currentTenant.getPgId()).document(newRoom.getId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(1));

        // Update tenant
        batch.update(tenantRepo.getTenantCollection().document(tenantId),
                "roomId", newRoom.getId(),
                "roomNumber", newRoom.getRoomNumber(),
                "bedId", newBed.getId(),
                "bedNumber", newBed.getBedNumber());

        batch.commit()
                .addOnSuccessListener(a -> Toast.makeText(this,
                        "✅ Moved to Room " + newRoom.getRoomNumber() + " / Bed " + newBed.getBedNumber(),
                        Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Transfer failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupRentHistory() {
        rentAdapter = new RentAdapter(new RentAdapter.OnRentActionListener() {
            @Override
            public void onMarkPaid(RentRecord record) {
                rentRepository.getRentCollection().document(record.getId())
                        .update("status", Constants.STATUS_PAID, "paidDate", Timestamp.now())
                        .addOnSuccessListener(a -> Toast.makeText(TenantDetailActivity.this, "Marked paid", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onRemind(RentRecord record) {
                String msg = WhatsAppHelper.getRentReminderMessage(record.getTenantName(), record.getAmount(), record.getPgName());
                WhatsAppHelper.sendWhatsAppMessage(TenantDetailActivity.this, record.getWhatsappNumber(), msg);
            }
        });
        binding.rvRentHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRentHistory.setAdapter(rentAdapter);
        rentRepository.getRentHistoryForTenant(tenantId).addSnapshotListener((value, error) -> {
            if (value != null) rentAdapter.setRentList(value.toObjects(RentRecord.class));
        });
    }

    private void showDeleteTenantDialog() {
        if (currentTenant == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Tenant")
                .setMessage("This will permanently delete " + currentTenant.getName() + "'s record, free their bed, and remove all rent history. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteTenantCascade())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTenantCascade() {
        binding.btnDeleteTenant.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // Free the bed
        batch.update(
                pgRepo.getBedCollection(currentTenant.getPgId(), currentTenant.getRoomId())
                        .document(currentTenant.getBedId()),
                "status", Constants.BED_VACANT, "tenantId", null, "tenantName", null);

        // Decrement PG occupiedBeds
        batch.update(
                pgRepo.getPGCollection().document(currentTenant.getPgId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(-1));

        // Decrement room occupiedBeds
        batch.update(
                pgRepo.getRoomCollection(currentTenant.getPgId()).document(currentTenant.getRoomId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(-1));

        // Delete tenant document
        batch.delete(tenantRepo.getTenantCollection().document(tenantId));

        batch.commit().addOnSuccessListener(a -> {
            // Delete all rent records for this tenant
            db.collection(Constants.COLLECTION_RENT_RECORDS)
                    .whereEqualTo("tenantId", tenantId)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            WriteBatch rentBatch = db.batch();
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap)
                                rentBatch.delete(doc.getReference());
                            rentBatch.commit();
                        }
                        Toast.makeText(this, "Tenant deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }).addOnFailureListener(e -> {
            binding.btnDeleteTenant.setEnabled(true);
            Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void startNoticePeriod() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 30);
        tenantRepo.getTenantCollection().document(tenantId).update(
                "status", Constants.STATUS_NOTICE,
                "noticeDate", Timestamp.now(),
                "expectedMoveOutDate", new Timestamp(cal.getTime())
        ).addOnSuccessListener(a -> Toast.makeText(this, "Notice period started", Toast.LENGTH_SHORT).show());
    }

    private void completeMoveOut() {
        if (currentTenant == null) { Toast.makeText(this, "Tenant data not loaded yet", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this)
                .setTitle("Complete Move-Out")
                .setMessage("Confirm move-out for " + currentTenant.getName() + "? Their bed will be freed.")
                .setPositiveButton("Confirm", (d, w) -> {
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    batch.update(tenantRepo.getTenantCollection().document(tenantId), "status", Constants.STATUS_MOVED_OUT);
                    batch.update(pgRepo.getBedCollection(currentTenant.getPgId(), currentTenant.getRoomId())
                            .document(currentTenant.getBedId()), "status", Constants.BED_VACANT, "tenantId", null, "tenantName", null);
                    batch.update(pgRepo.getPGCollection().document(currentTenant.getPgId()),
                            "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(-1));
                    batch.update(pgRepo.getRoomCollection(currentTenant.getPgId()).document(currentTenant.getRoomId()),
                            "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(-1));
                    batch.commit().addOnSuccessListener(a -> {
                        Toast.makeText(this, "Move-out completed", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
