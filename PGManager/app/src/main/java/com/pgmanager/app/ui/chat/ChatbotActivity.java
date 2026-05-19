package com.pgmanager.app.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pgmanager.app.data.model.ChatMessage;
import com.pgmanager.app.databinding.ActivityChatbotBinding;
import com.pgmanager.app.ui.expense.AddExpenseActivity;
import com.pgmanager.app.ui.pg.AddEditPGActivity;
import com.pgmanager.app.ui.tenant.AddEditTenantActivity;
import com.pgmanager.app.util.Constants;

public class ChatbotActivity extends AppCompatActivity {

    private ActivityChatbotBinding binding;
    private ChatAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int STATE_MAIN     = 0;
    private static final int STATE_PG       = 1;
    private static final int STATE_TENANT   = 2;
    private static final int STATE_RENT     = 3;
    private static final int STATE_EXPENSE  = 4;
    private static final int STATE_FOOD     = 5;
    private static final int STATE_ROOMS    = 6;
    private int currentState = STATE_MAIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ChatAdapter();
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                binding.etMessage.setText("");
                handleUserInput(text);
            }
        });

        botSay("👋 Hi! I'm your PG Assistant.\n\nI know everything about this app and can guide you through any task. What would you like to do?");
        showMainOptions();
    }

    private void botSay(String text) {
        adapter.addMessage(new ChatMessage(text, ChatMessage.TYPE_BOT));
        binding.rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    private void userSay(String text) {
        adapter.addMessage(new ChatMessage(text, ChatMessage.TYPE_USER));
        binding.rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    private void botSayDelayed(String text, int ms) {
        handler.postDelayed(() -> botSay(text), ms);
    }

    private void showOptions(String... options) {
        binding.llOptions.removeAllViews();
        for (String option : options) {
            Chip chip = new Chip(this);
            chip.setText(option);
            chip.setChipBackgroundColorResource(com.pgmanager.app.R.color.surface_variant);
            chip.setTextColor(getResources().getColor(com.pgmanager.app.R.color.text_primary));
            chip.setOnClickListener(v -> { userSay(option); handleUserInput(option); });
            binding.llOptions.addView(chip);
        }
        binding.scrollOptions.setVisibility(View.VISIBLE);
    }

    private void clearOptions() {
        binding.llOptions.removeAllViews();
        binding.scrollOptions.setVisibility(View.GONE);
    }

    private void showMainOptions() {
        currentState = STATE_MAIN;
        showOptions("🏠 PG Management", "👤 Tenants", "💰 Rent", "💸 Expenses", "🍽️ Food Menu", "📊 Expense Tracker", "❓ Help");
    }

    private void handleUserInput(String input) {
        clearOptions();
        String lower = input.toLowerCase();
        switch (currentState) {
            case STATE_MAIN:    handleMain(lower); break;
            case STATE_PG:      handlePG(lower); break;
            case STATE_TENANT:  handleTenant(lower); break;
            case STATE_RENT:    handleRent(lower); break;
            case STATE_EXPENSE: handleExpense(lower); break;
            case STATE_FOOD:    handleFood(lower); break;
            case STATE_ROOMS:   handleRooms(lower); break;
            default:            handleMain(lower);
        }
    }

    private void handleMain(String lower) {
        if (lower.contains("pg") || lower.contains("propert") || lower.contains("management")) {
            currentState = STATE_PG;
            botSay("🏠 PG Management — what would you like to do?");
            showOptions("➕ Add New PG", "📋 View My PGs", "🚪 Rooms & Beds", "✏️ Edit PG", "🗑️ Delete PG", "⬅️ Back");
        } else if (lower.contains("tenant")) {
            currentState = STATE_TENANT;
            botSay("👤 Tenant Management — what would you like to do?");
            showOptions("➕ Add Tenant", "📋 View Tenants", "✏️ Edit Tenant", "📞 Update Phone", "⚠️ Notice Period", "🚪 Move Out", "⬅️ Back");
        } else if (lower.contains("rent") || lower.contains("collect") || lower.contains("payment")) {
            currentState = STATE_RENT;
            botSay("💰 Rent Collection — what would you like to do?");
            showOptions("📊 View Rent Status", "✅ Mark Paid", "📲 Send Reminder", "🔴 Overdue Rents", "⬅️ Back");
        } else if (lower.contains("expense")) {
            currentState = STATE_EXPENSE;
            botSay("💸 Expenses — what would you like to do?");
            showOptions("➕ Add Expense", "📋 View Expenses", "⬅️ Back");
        } else if (lower.contains("food") || lower.contains("menu") || lower.contains("meal")) {
            currentState = STATE_FOOD;
            botSay("🍽️ Food Menu — what would you like to do?");
            showOptions("📝 Set Tomorrow's Menu", "📲 Send Menu to Group", "🔗 Add WhatsApp Group Link", "⬅️ Back");
        } else if (lower.contains("tracker") || lower.contains("monthly") || lower.contains("total expense") || lower.contains("setting")) {
            botSay("📊 Monthly Expense Tracker is in the *Settings* tab.\n\nIt shows:\n• Total expenses across ALL your PGs\n• Breakdown per PG\n\nTap *↻ Refresh* to reload the latest data from Firestore.\n\nExpenses are pulled from the *View Expenses* section of each PG.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("help") || lower.contains("?") || lower.contains("what")) {
            botSay("❓ Here's everything this app can do:\n\n🏠 *PG Management* — Add/edit/delete PGs, manage rooms & beds\n\n👤 *Tenants* — Add tenants, assign beds, edit name/phone/rent, start notice period, complete move-out\n\n💰 *Rent* — View pending/paid/overdue rents, mark as paid, send WhatsApp reminders\n\n💸 *Expenses* — Track electricity, repairs, salary, water etc. per PG\n\n📊 *Expense Tracker* — See total monthly expenses across all PGs in Settings tab\n\n🍽️ *Food Menu* — Set tomorrow's meals and send to WhatsApp group in one tap\n\n🚪 *Rooms & Beds* — See which beds are occupied and by whom\n\nJust tap an option or type what you need!");
            handler.postDelayed(this::showMainOptions, 500);
        } else {
            botSay("I didn't quite get that. Here's what I can help with:");
            showMainOptions();
        }
    }

    private void handlePG(String lower) {
        if (lower.contains("add") || lower.contains("new") || lower.contains("create")) {
            botSay("Opening Add PG screen... 🏠\n\nFill in:\n• PG Name\n• Address\n• Default monthly rent\n• Default advance amount");
            handler.postDelayed(() -> startActivity(new Intent(this, AddEditPGActivity.class)), 800);
            botSayDelayed("Once saved, tap on the PG to add rooms and beds.", 1000);
            handler.postDelayed(this::showMainOptions, 1200);
        } else if (lower.contains("view") || lower.contains("list") || lower.contains("my pg")) {
            botSay("📋 Go to the *My PGs* tab at the bottom.\n\nTap any PG card to open its detail page where you can manage rooms, view tenants, add expenses, and set the food menu.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("room") || lower.contains("bed")) {
            currentState = STATE_ROOMS;
            botSay("🚪 Rooms & Beds — what would you like to know?");
            showOptions("➕ Add a Room", "👁️ View Occupied Beds", "🛏️ Bed Status", "⬅️ Back");
        } else if (lower.contains("edit") || lower.contains("update")) {
            botSay("✏️ To edit a PG:\n\n1. Go to *My PGs* tab\n2. Tap on the PG\n3. Tap *Edit PG* button\n4. Update name, address, or rent\n5. Tap Save");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("delete")) {
            botSay("🗑️ To delete a PG:\n\n1. Go to *My PGs* tab\n2. Tap on the PG\n3. Tap *Delete PG* button\n4. Confirm deletion\n\n⚠️ This removes all room and bed data for that property.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            botSay("Back to main menu 👍");
            showMainOptions();
        } else {
            showOptions("➕ Add New PG", "📋 View My PGs", "🚪 Rooms & Beds", "✏️ Edit PG", "🗑️ Delete PG", "⬅️ Back");
        }
    }

    private void handleRooms(String lower) {
        if (lower.contains("add") || lower.contains("new")) {
            botSay("➕ To add a room:\n\n1. Go to *My PGs* tab\n2. Tap on your PG\n3. Tap *Manage Rooms*\n4. Tap the ➕ FAB button\n5. Enter room number, floor, and number of beds\n6. Tap Add Room\n\nBeds are created automatically!");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("occupied") || lower.contains("who") || lower.contains("tenant")) {
            botSay("👁️ To see who is in each bed:\n\n1. Go to *My PGs* tab\n2. Tap on your PG\n3. Tap *Manage Rooms*\n\nEach bed card shows:\n🛏️ Vacant — empty bed\n👤 Tenant's name — occupied bed\n\nTap an occupied bed to go to that tenant's profile.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("status") || lower.contains("bed")) {
            botSay("🛏️ Bed status is shown in the Rooms view:\n\n• *Vacant* — available for new tenant\n• *Tenant name* — currently occupied\n\nTap a vacant bed to add a new tenant directly.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            currentState = STATE_PG;
            botSay("Back to PG options 👍");
            showOptions("➕ Add New PG", "📋 View My PGs", "🚪 Rooms & Beds", "✏️ Edit PG", "🗑️ Delete PG", "⬅️ Back");
        } else {
            showOptions("➕ Add a Room", "👁️ View Occupied Beds", "🛏️ Bed Status", "⬅️ Back");
        }
    }

    private void handleTenant(String lower) {
        if (lower.contains("add") || lower.contains("new")) {
            botSay("Opening Add Tenant screen... 👤\n\nYou'll need to:\n1. Enter tenant name & phone\n2. Select PG\n3. Select Room\n4. Select a vacant Bed\n5. Set joining date & rent amount");
            handler.postDelayed(() -> startActivity(new Intent(this, AddEditTenantActivity.class)), 800);
            botSayDelayed("Make sure you have a PG with at least one vacant bed first!", 1000);
            handler.postDelayed(this::showMainOptions, 1200);
        } else if (lower.contains("view") || lower.contains("list")) {
            botSay("📋 Go to the *Tenants* tab at the bottom.\n\nYou'll see all active tenants with their PG, room, rent amount, and status badge.\n\nTap any tenant to view full details.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("edit") || lower.contains("update") || lower.contains("phone")) {
            botSay("✏️ To edit a tenant's details:\n\n1. Go to *Tenants* tab\n2. Tap on the tenant\n3. Tap the *Edit* button\n4. Update name, phone number, or rent amount\n5. Tap Save\n\nChanges are saved instantly to the cloud.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("notice")) {
            botSay("⚠️ To start a notice period:\n\n1. Go to *Tenants* tab\n2. Tap on the tenant\n3. Tap *Notice* button\n\nA 30-day countdown starts automatically.\nThe tenant's status changes to NOTICE.\nYou'll see the countdown and expected move-out date.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("move") || lower.contains("out") || lower.contains("vacate")) {
            botSay("🚪 To complete a move-out:\n\n1. Go to *Tenants* tab\n2. Tap on the tenant (must be on Notice)\n3. Tap *Complete Move-Out*\n\nThis will:\n✅ Mark tenant as moved out\n✅ Free the bed for new tenant\n✅ Update PG occupancy count");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            botSay("Back to main menu 👍");
            showMainOptions();
        } else {
            showOptions("➕ Add Tenant", "📋 View Tenants", "✏️ Edit Tenant", "📞 Update Phone", "⚠️ Notice Period", "🚪 Move Out", "⬅️ Back");
        }
    }

    private void handleRent(String lower) {
        if (lower.contains("mark") || lower.contains("paid")) {
            botSay("✅ To mark rent as paid:\n\n*From Dashboard:*\nPending dues are shown directly — tap *✓ Mark Paid*\n\n*From Rent tab:*\n1. Go to *Rent* tab\n2. Find the tenant's record\n3. Tap *✓ Mark Paid*\n\nThe record moves to Paid status instantly.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("remind") || lower.contains("whatsapp") || lower.contains("message")) {
            botSay("📲 To send a WhatsApp reminder:\n\n1. Go to *Rent* tab\n2. Find a pending/overdue record\n3. Tap *💬 Remind*\n\nWhatsApp opens with a pre-written message:\n\"Hi [Name], your rent of ₹X for [PG] is due...\"\n\nJust tap Send!");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("view") || lower.contains("status") || lower.contains("all")) {
            botSay("📊 Go to the *Rent* tab at the bottom.\n\nFilter by:\n🟣 *All* — every rent record\n🟡 *Pending* — not yet paid\n🟢 *Paid* — confirmed payments\n🔴 *Overdue* — past due date\n\nEach card shows tenant name, PG, amount, due date, and action buttons.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("overdue")) {
            botSay("🔴 To view overdue rents:\n\n1. Go to *Rent* tab\n2. Tap the *Overdue* filter chip\n\nYou'll see all tenants who haven't paid past the due date.\nTap *💬 Remind* to send them a WhatsApp message.");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            botSay("Back to main menu 👍");
            showMainOptions();
        } else {
            showOptions("📊 View Rent Status", "✅ Mark Paid", "📲 Send Reminder", "🔴 Overdue Rents", "⬅️ Back");
        }
    }

    private void handleExpense(String lower) {
        if (lower.contains("add") || lower.contains("new")) {
            botSay("Opening Add Expense screen... 💸");
            loadFirstPGAndOpenExpense();
        } else if (lower.contains("view") || lower.contains("list")) {
            botSay("📋 To view expenses for a PG:\n\n1. Go to *My PGs* tab\n2. Tap on your PG\n3. Tap *View Expenses*\n\nYou'll see all expenses with:\n• Category (Electricity, Repairs, etc.)\n• Amount\n• Description\n• Date");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            botSay("Back to main menu 👍");
            showMainOptions();
        } else {
            showOptions("➕ Add Expense", "📋 View Expenses", "⬅️ Back");
        }
    }

    private void handleFood(String lower) {
        if (lower.contains("set") || lower.contains("menu") || lower.contains("tomorrow") || lower.contains("meal")) {
            botSay("🍽️ To set tomorrow's menu:\n\n1. Go to *My PGs* tab\n2. Tap on your PG\n3. Scroll down to *Tomorrow's Menu* card\n4. Fill in Breakfast, Lunch, and Dinner\n5. Tap *Send Menu to WhatsApp Group*\n\nThe menu is saved automatically for next time!");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("send") || lower.contains("group") || lower.contains("whatsapp")) {
            botSay("📲 To send the menu to your WhatsApp group:\n\n*First time setup:*\n1. Open your PG's WhatsApp group\n2. Tap ⋮ → Invite via link → Copy link\n3. In the app, tap *+ Add Group Link* in the menu card\n4. Paste the link and save\n\n*Every day:*\n1. Fill in the meals\n2. Tap *Send Menu to WhatsApp Group*\n3. WhatsApp opens the group — just paste and send!\n\nThe message is auto-copied to your clipboard 📋");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("link") || lower.contains("add")) {
            botSay("🔗 To add your WhatsApp group link:\n\n1. Open WhatsApp\n2. Go to your PG group\n3. Tap the group name → Invite via link → Copy link\n4. In the app, go to PG Detail\n5. Tap *+ Add Group Link*\n6. Paste and save\n\nOnce linked, the button shows ✅ Group Linked");
            handler.postDelayed(this::showMainOptions, 500);
        } else if (lower.contains("back") || lower.contains("⬅")) {
            botSay("Back to main menu 👍");
            showMainOptions();
        } else {
            showOptions("📝 Set Tomorrow's Menu", "📲 Send Menu to Group", "🔗 Add WhatsApp Group Link", "⬅️ Back");
        }
    }

    private void loadFirstPGAndOpenExpense() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { botSay("Please log in first."); return; }
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PGS)
                .whereEqualTo("ownerId", uid).limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String pgId = snap.getDocuments().get(0).getId();
                        Intent intent = new Intent(this, AddExpenseActivity.class);
                        intent.putExtra(Constants.EXTRA_PG_ID, pgId);
                        startActivity(intent);
                        botSayDelayed("Select category, enter amount and description, then tap Save.", 500);
                    } else {
                        botSay("You don't have any PGs yet.\nAdd a PG first from the My PGs tab.");
                    }
                    handler.postDelayed(this::showMainOptions, 1000);
                });
    }
}
