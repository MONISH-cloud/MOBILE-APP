package com.pgmanager.app.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pgmanager.app.data.model.AppNotification;
import com.pgmanager.app.databinding.ActivityNotificationsBinding;
import com.pgmanager.app.util.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private FirebaseFirestore db;
    private NotifAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();

        adapter = new NotifAdapter(new ArrayList<>());
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        db.collection("notifications").whereEqualTo("userId", uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<AppNotification> list = snap.toObjects(AppNotification.class);
                    adapter.setList(list);
                    binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void markAllRead() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        db.collection("notifications").whereEqualTo("userId", uid).whereEqualTo("read", false).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("read", true);
                    Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show();
                });
    }

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private List<AppNotification> list;
        NotifAdapter(List<AppNotification> list) { this.list = list; }
        void setList(List<AppNotification> l) { this.list = l; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(com.pgmanager.app.R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppNotification n = list.get(pos);
            h.tvIcon.setText(n.getIcon() != null ? n.getIcon() : "🔔");
            h.tvTitle.setText(n.getTitle());
            h.tvMessage.setText(n.getMessage());
            h.tvTime.setText(DateUtils.formatTimestamp(n.getCreatedAt()));
            h.unreadBar.setVisibility(!n.isRead() ? View.VISIBLE : View.GONE);
            if (!n.isRead()) {
                h.itemView.setOnClickListener(v ->
                        db.collection("notifications").document(n.getId()).update("read", true));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvMessage, tvTime;
            View unreadBar;
            VH(View v) {
                super(v);
                tvIcon = v.findViewById(com.pgmanager.app.R.id.tvIcon);
                tvTitle = v.findViewById(com.pgmanager.app.R.id.tvTitle);
                tvMessage = v.findViewById(com.pgmanager.app.R.id.tvMessage);
                tvTime = v.findViewById(com.pgmanager.app.R.id.tvTime);
                unreadBar = v.findViewById(com.pgmanager.app.R.id.unreadBar);
            }
        }
    }
}
