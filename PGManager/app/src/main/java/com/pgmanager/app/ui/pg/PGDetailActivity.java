package com.pgmanager.app.ui.pg;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.model.Tenant;
import com.pgmanager.app.data.repository.PGRepository;
import com.pgmanager.app.data.repository.TenantRepository;
import com.pgmanager.app.databinding.ActivityPgDetailBinding;
import com.pgmanager.app.ui.expense.AddExpenseActivity;
import com.pgmanager.app.ui.room.RoomBedActivity;
import com.pgmanager.app.ui.tenant.TenantAdapter;
import com.pgmanager.app.ui.tenant.TenantDetailActivity;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PGDetailActivity extends AppCompatActivity {
    private ActivityPgDetailBinding binding;
    private PGRepository pgRepo;
    private TenantRepository tenantRepo;
    private String pgId;
    private PG currentPG;
    private TenantAdapter tenantAdapter;
    private String whatsappGroupLink = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPgDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pgRepo = new PGRepository();
        tenantRepo = new TenantRepository();
        pgId = getIntent().getStringExtra(Constants.EXTRA_PG_ID);

        setupTenantsList();
        loadPGDetails();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnManageRooms.setOnClickListener(v -> {
            Intent i = new Intent(this, RoomBedActivity.class);
            i.putExtra(Constants.EXTRA_PG_ID, pgId);
            startActivity(i);
        });
        binding.btnViewExpenses.setOnClickListener(v -> {
            Intent i = new Intent(this, AddExpenseActivity.class);
            i.putExtra(Constants.EXTRA_PG_ID, pgId);
            startActivity(i);
        });
        binding.btnEditPG.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditPGActivity.class);
            i.putExtra(Constants.EXTRA_PG_ID, pgId);
            startActivity(i);
        });
        binding.btnDeletePG.setOnClickListener(v -> showDeleteDialog());
        binding.tvGroupLink.setOnClickListener(v -> showGroupLinkDialog());
        binding.btnSendMenu.setOnClickListener(v -> sendMenuToGroup());
    }

    private void loadPGDetails() {
        pgRepo.getPGCollection().document(pgId).addSnapshotListener((snapshot, error) -> {
            if (snapshot == null || !snapshot.exists()) return;
            currentPG = snapshot.toObject(PG.class);
            if (currentPG != null) {
                binding.tvPGName.setText(currentPG.getName());
                binding.tvPGAddress.setText(currentPG.getAddress());
                binding.tvRooms.setText(currentPG.getTotalRooms() + " Rooms");
                binding.tvBeds.setText(currentPG.getOccupiedBeds() + "/" + currentPG.getTotalBeds() + " Beds");
                binding.tvDefaultRent.setText(CurrencyUtils.formatCurrency(currentPG.getDefaultRent()) + "/mo");
            }
            String link = snapshot.getString("whatsappGroupLink");
            if (link != null && !link.isEmpty()) {
                whatsappGroupLink = link;
                binding.tvGroupLink.setText("✅ Group Linked");
            }
            String bf = snapshot.getString("menuBreakfast");
            String ln = snapshot.getString("menuLunch");
            String dn = snapshot.getString("menuDinner");
            if (bf != null) binding.etBreakfast.setText(bf);
            if (ln != null) binding.etLunch.setText(ln);
            if (dn != null) binding.etDinner.setText(dn);
        });
    }

    private void setupTenantsList() {
        tenantAdapter = new TenantAdapter(tenant -> {
            Intent i = new Intent(this, TenantDetailActivity.class);
            i.putExtra(Constants.EXTRA_TENANT_ID, tenant.getId());
            startActivity(i);
        });
        binding.rvTenants.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTenants.setAdapter(tenantAdapter);

        // Query tenants for this PG that are not moved out
        tenantRepo.getTenantCollection()
                .whereEqualTo("pgId", pgId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Tenant> tenants = new ArrayList<>();
                        for (Tenant t : value.toObjects(Tenant.class)) {
                            if (!Constants.STATUS_MOVED_OUT.equals(t.getStatus()))
                                tenants.add(t);
                        }
                        tenantAdapter.setTenantList(tenants);
                    }
                });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete PG")
                .setMessage("This will permanently delete the PG, all rooms, beds, and tenant records. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePGCascade())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePGCascade() {
        binding.btnDeletePG.setEnabled(false);
        binding.btnDeletePG.setText("Deleting...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Delete all rooms and their beds
        pgRepo.getRoomCollection(pgId).get().addOnSuccessListener(roomSnap -> {
            if (roomSnap.isEmpty()) {
                // No rooms — delete expenses then PG
                deleteExpensesAndPG(db);
                return;
            }

            AtomicInteger roomsDone = new AtomicInteger(0);
            int roomCount = roomSnap.size();

            for (QueryDocumentSnapshot roomDoc : roomSnap) {
                String roomId = roomDoc.getId();
                // Delete all beds in this room
                pgRepo.getBedCollection(pgId, roomId).get().addOnSuccessListener(bedSnap -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot bedDoc : bedSnap) {
                        batch.delete(bedDoc.getReference());
                    }
                    batch.delete(pgRepo.getRoomCollection(pgId).document(roomId));
                    batch.commit().addOnSuccessListener(a -> {
                        if (roomsDone.incrementAndGet() == roomCount) {
                            deleteExpensesAndPG(db);
                        }
                    }).addOnFailureListener(e -> showDeleteError(e.getMessage()));
                }).addOnFailureListener(e -> showDeleteError(e.getMessage()));
            }
        }).addOnFailureListener(e -> showDeleteError(e.getMessage()));
    }

    private void deleteExpensesAndPG(FirebaseFirestore db) {
        // Step 2: Delete expenses subcollection
        pgRepo.getPGCollection().document(pgId)
                .collection(Constants.COLLECTION_EXPENSES).get()
                .addOnSuccessListener(expSnap -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : expSnap) batch.delete(doc.getReference());
                    // Step 3: Delete the PG document itself
                    batch.delete(pgRepo.getPGCollection().document(pgId));
                    batch.commit().addOnSuccessListener(a -> {
                        // Step 4: Mark tenants of this PG as moved out
                        tenantRepo.getTenantCollection()
                                .whereEqualTo("pgId", pgId)
                                .get()
                                .addOnSuccessListener(tenantSnap -> {
                                    if (!tenantSnap.isEmpty()) {
                                        WriteBatch tb = db.batch();
                                        for (QueryDocumentSnapshot t : tenantSnap)
                                            tb.update(t.getReference(), "status", Constants.STATUS_MOVED_OUT);
                                        tb.commit();
                                    }
                                    Toast.makeText(this, "PG deleted successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }).addOnFailureListener(e -> showDeleteError(e.getMessage()));
                }).addOnFailureListener(e -> showDeleteError(e.getMessage()));
    }

    private void showDeleteError(String msg) {
        binding.btnDeletePG.setEnabled(true);
        binding.btnDeletePG.setText("🗑️ Delete");
        Toast.makeText(this, "Delete failed: " + msg, Toast.LENGTH_LONG).show();
    }

    private void showGroupLinkDialog() {
        EditText et = new EditText(this);
        et.setHint("Paste WhatsApp group invite link");
        et.setText(whatsappGroupLink);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(48, 24, 48, 8);
        layout.addView(et);

        new AlertDialog.Builder(this)
                .setTitle("WhatsApp Group Link")
                .setMessage("Open group → ⋮ → Invite via link → Copy link")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    whatsappGroupLink = et.getText().toString().trim();
                    if (!whatsappGroupLink.isEmpty()) {
                        pgRepo.getPGCollection().document(pgId)
                                .update("whatsappGroupLink", whatsappGroupLink)
                                .addOnSuccessListener(a -> {
                                    binding.tvGroupLink.setText("✅ Group Linked");
                                    Toast.makeText(this, "Group link saved!", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMenuToGroup() {
        String breakfast = binding.etBreakfast.getText().toString().trim();
        String lunch = binding.etLunch.getText().toString().trim();
        String dinner = binding.etDinner.getText().toString().trim();

        if (breakfast.isEmpty() && lunch.isEmpty() && dinner.isEmpty()) {
            Toast.makeText(this, "Please fill at least one meal", Toast.LENGTH_SHORT).show();
            return;
        }

        pgRepo.getPGCollection().document(pgId).update(
                "menuBreakfast", breakfast, "menuLunch", lunch, "menuDinner", dinner);

        String pgName = currentPG != null ? currentPG.getName() : "PG";
        StringBuilder msg = new StringBuilder("🍽️ *Tomorrow's Menu — ").append(pgName).append("*\n\n");
        if (!breakfast.isEmpty()) msg.append("🌅 *Breakfast:* ").append(breakfast).append("\n");
        if (!lunch.isEmpty()) msg.append("☀️ *Lunch:* ").append(lunch).append("\n");
        if (!dinner.isEmpty()) msg.append("🌙 *Dinner:* ").append(dinner).append("\n");
        msg.append("\n_— PG Management_");

        if (!whatsappGroupLink.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(whatsappGroupLink));
                intent.setPackage("com.whatsapp");
                startActivity(intent);
                android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cb.setPrimaryClip(android.content.ClipData.newPlainText("menu", msg.toString()));
                Toast.makeText(this, "Group opened! Message copied — paste & send 📋", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                openWhatsAppWithMessage(msg.toString());
            }
        } else {
            openWhatsAppWithMessage(msg.toString());
            Toast.makeText(this, "Select your PG group in WhatsApp to send", Toast.LENGTH_LONG).show();
        }
    }

    private void openWhatsAppWithMessage(String message) {
        try {
            String encoded = URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + encoded));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
