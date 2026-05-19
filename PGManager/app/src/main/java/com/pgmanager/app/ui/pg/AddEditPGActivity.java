package com.pgmanager.app.ui.pg;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.databinding.ActivityAddEditPgBinding;
import com.pgmanager.app.util.Constants;
import java.util.ArrayList;

public class AddEditPGActivity extends AppCompatActivity {
    private ActivityAddEditPgBinding binding;
    private PGRepository pgRepo;
    private String pgId;
    private double pickedLat = 0, pickedLng = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditPgBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pgRepo = new PGRepository();
        pgId = getIntent().getStringExtra(Constants.EXTRA_PG_ID);

        if (pgId != null) {
            binding.tvTitle.setText("Edit PG");
            loadPG();
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> savePG());
        binding.btnPickLocation.setOnClickListener(v -> openMapPicker());
    }

    private void openMapPicker() {
        // Open Google Maps in search mode — user searches/pins their location
        // We use geo intent; if Maps is installed it opens directly
        String query = binding.etAddress.getText().toString().trim();
        Uri uri = query.isEmpty()
                ? Uri.parse("geo:12.9716,77.5946?q=Bangalore") // default to Bangalore center
                : Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            // Maps is installed — open it, user can long-press to drop a pin
            // then share the location back. We show instructions.
            startActivity(intent);
            Toast.makeText(this,
                    "Long-press on your PG location → tap Share → copy coordinates",
                    Toast.LENGTH_LONG).show();
            showManualCoordEntry();
        } else {
            // Maps not installed — show manual entry
            showManualCoordEntry();
        }
    }

    private void showManualCoordEntry() {
        // Show a dialog to paste lat,lng manually
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("e.g. 12.9716, 77.5946");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        if (pickedLat != 0) et.setText(pickedLat + ", " + pickedLng);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setPadding(56, 32, 56, 8);
        layout.addView(et);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📍 Enter Coordinates")
                .setMessage("Open Google Maps → long-press your PG location → tap the address bar at bottom → copy the coordinates shown (e.g. 12.9716, 77.5946)")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String input = et.getText().toString().trim();
                    if (!parseLatLng(input)) {
                        Toast.makeText(this, "Invalid format. Use: 12.9716, 77.5946", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean parseLatLng(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 2) return false;
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return false;
            pickedLat = lat;
            pickedLng = lng;
            updateLocationStatus();
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateLocationStatus() {
        if (pickedLat != 0 && pickedLng != 0) {
            binding.tvLocationStatus.setText("✅ Location set: " + String.format("%.4f, %.4f", pickedLat, pickedLng));
            binding.tvLocationStatus.setTextColor(getResources().getColor(com.pgmanager.app.R.color.accent));
        }
    }

    private void loadPG() {
        pgRepo.getPGCollection().document(pgId).get().addOnSuccessListener(doc -> {
            PG pg = doc.toObject(PG.class);
            if (pg == null) return;
            binding.etPGName.setText(pg.getName());
            binding.etAddress.setText(pg.getAddress());
            binding.etDefaultRent.setText(String.valueOf((int) pg.getDefaultRent()));
            binding.etDefaultAdvance.setText(String.valueOf((int) pg.getDefaultAdvance()));
            if (pg.getPhotoUrl() != null) binding.etPhotoUrl.setText(pg.getPhotoUrl());
            if (pg.getLatitude() != 0 && pg.getLongitude() != 0) {
                pickedLat = pg.getLatitude();
                pickedLng = pg.getLongitude();
                updateLocationStatus();
            }
        });
    }

    private void savePG() {
        String name    = binding.etPGName.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String rentStr = binding.etDefaultRent.getText().toString().trim();
        String advStr  = binding.etDefaultAdvance.getText().toString().trim();

        if (name.isEmpty()) { binding.tilPGName.setError("Required"); return; }

        String photoUrl = binding.etPhotoUrl.getText().toString().trim();
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        if (pgId == null) {
            PG pg = new PG();
            pg.setId(pgRepo.getPGCollection().document().getId());
            pg.setOwnerId(FirebaseAuth.getInstance().getUid());
            pg.setManagerIds(new ArrayList<>());
            pg.setName(name);
            pg.setAddress(address);
            pg.setDefaultRent(rentStr.isEmpty() ? 0 : Double.parseDouble(rentStr));
            pg.setDefaultAdvance(advStr.isEmpty() ? 0 : Double.parseDouble(advStr));
            pg.setPhotoUrl(photoUrl.isEmpty() ? null : photoUrl);
            pg.setLatitude(pickedLat);
            pg.setLongitude(pickedLng);
            pg.setCreatedAt(Timestamp.now());
            pg.setUpdatedAt(Timestamp.now());
            pgRepo.getPGCollection().document(pg.getId()).set(pg)
                    .addOnSuccessListener(a -> finish())
                    .addOnFailureListener(e -> showError(e.getMessage()));
        } else {
            pgRepo.getPGCollection().document(pgId).update(
                    "name", name,
                    "address", address,
                    "defaultRent", rentStr.isEmpty() ? 0 : Double.parseDouble(rentStr),
                    "defaultAdvance", advStr.isEmpty() ? 0 : Double.parseDouble(advStr),
                    "photoUrl", photoUrl.isEmpty() ? null : photoUrl,
                    "latitude", pickedLat,
                    "longitude", pickedLng,
                    "updatedAt", Timestamp.now()
            ).addOnSuccessListener(a -> finish())
             .addOnFailureListener(e -> showError(e.getMessage()));
        }
    }

    private void showError(String msg) {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnSave.setEnabled(true);
        Toast.makeText(this, "Failed: " + msg, Toast.LENGTH_LONG).show();
    }
}
