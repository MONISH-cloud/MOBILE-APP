package com.pgmanager.app.ui.tenant;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.pgmanager.app.R;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.model.Room;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.data.repository.TenantRepository;
import com.pgmanager.app.databinding.ActivityAddEditTenantBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddEditTenantActivity extends AppCompatActivity {
    private ActivityAddEditTenantBinding binding;
    private PGRepository pgRepo;
    private TenantRepository tenantRepo;
    private Calendar selectedDate = Calendar.getInstance();
    private List<PG> pgList = new ArrayList<>();
    private List<Room> roomList = new ArrayList<>();
    private List<Bed> bedList = new ArrayList<>();
    
    private PG selectedPG;
    private Room selectedRoom;
    private Bed selectedBed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditTenantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pgRepo = new PGRepository();
        tenantRepo = new TenantRepository();

        setupDropdowns();
        setupDatePicker();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveTenant());
    }

    private void setupDropdowns() {
        // Load PGs
        pgRepo.getMyPGs(com.google.firebase.auth.FirebaseAuth.getInstance().getUid()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            pgList = queryDocumentSnapshots.toObjects(PG.class);
            List<String> pgNames = new ArrayList<>();
            for (PG pg : pgList) pgNames.add(pg.getName());
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, pgNames);
            binding.spinnerPG.setAdapter(adapter);
        });

        binding.spinnerPG.setOnItemClickListener((parent, view, position, id) -> {
            selectedPG = pgList.get(position);
            loadRooms(selectedPG.getId());
        });

        binding.spinnerRoom.setOnItemClickListener((parent, view, position, id) -> {
            selectedRoom = roomList.get(position);
            loadBeds(selectedPG.getId(), selectedRoom.getId());
        });

        binding.spinnerBed.setOnItemClickListener((parent, view, position, id) -> {
            selectedBed = bedList.get(position);
        });
    }

    private void loadRooms(String pgId) {
        pgRepo.getRoomCollection(pgId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            roomList = queryDocumentSnapshots.toObjects(Room.class);
            List<String> roomNames = new ArrayList<>();
            for (Room r : roomList) roomNames.add("Room " + r.getRoomNumber());
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roomNames);
            binding.spinnerRoom.setAdapter(adapter);
            binding.spinnerRoom.setText("", false);
            binding.spinnerBed.setText("", false);
        });
    }

    private void loadBeds(String pgId, String roomId) {
        pgRepo.getBedCollection(pgId, roomId).whereEqualTo("status", Constants.BED_VACANT).get().addOnSuccessListener(queryDocumentSnapshots -> {
            bedList = queryDocumentSnapshots.toObjects(Bed.class);
            List<String> bedNames = new ArrayList<>();
            for (Bed b : bedList) bedNames.add("Bed " + b.getBedNumber());
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bedNames);
            binding.spinnerBed.setAdapter(adapter);
            binding.spinnerBed.setText("", false);
        });
    }

    private void setupDatePicker() {
        binding.etJoiningDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                binding.etJoiningDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void saveTenant() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etWhatsApp.getText().toString().trim();
        String rentStr = binding.etRent.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty() || phone.length() != 10) {
            Toast.makeText(this, "Enter valid 10-digit WhatsApp number — tenant needs this to see their PG details", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedPG == null) {
            Toast.makeText(this, "Please select a PG", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRoom == null) {
            Toast.makeText(this, "Please select a room", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBed == null) {
            Toast.makeText(this, "Please select a bed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rentStr.isEmpty()) {
            Toast.makeText(this, "Rent amount is required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        String tenantId = tenantRepo.getTenantCollection().document().getId();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName(name);
        tenant.setWhatsappNumber(phone);
        tenant.setPgId(selectedPG.getId());
        tenant.setPgName(selectedPG.getName());
        tenant.setRoomId(selectedRoom.getId());
        tenant.setRoomNumber(selectedRoom.getRoomNumber());
        tenant.setBedId(selectedBed.getId());
        tenant.setBedNumber(selectedBed.getBedNumber());
        tenant.setJoiningDate(new Timestamp(selectedDate.getTime()));
        try {
            tenant.setRentAmount(Double.parseDouble(rentStr));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid rent amount", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSave.setEnabled(true);
            return;
        }
        tenant.setStatus(Constants.STATUS_ACTIVE);
        tenant.setCreatedAt(Timestamp.now());

        batch.set(tenantRepo.getTenantCollection().document(tenantId), tenant);
        
        // Mark bed as occupied
        batch.update(pgRepo.getBedCollection(selectedPG.getId(), selectedRoom.getId()).document(selectedBed.getId()),
                "status", Constants.BED_OCCUPIED, "tenantId", tenantId, "tenantName", name);

        // Update room occupiedBeds
        batch.update(pgRepo.getRoomCollection(selectedPG.getId()).document(selectedRoom.getId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(1));

        // Update PG occupiedBeds
        batch.update(pgRepo.getPGCollection().document(selectedPG.getId()),
                "occupiedBeds", com.google.firebase.firestore.FieldValue.increment(1));

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Tenant Saved", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSave.setEnabled(true);
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
