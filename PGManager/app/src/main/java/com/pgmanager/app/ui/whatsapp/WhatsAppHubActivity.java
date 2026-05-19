package com.pgmanager.app.ui.whatsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pgmanager.app.data.model.RentRecord;
import com.pgmanager.app.databinding.ActivityWhatsappHubBinding;
import com.pgmanager.app.util.Constants;
import com.pgmanager.app.util.CurrencyUtils;
import com.pgmanager.app.util.DateUtils;
import com.pgmanager.app.util.WhatsAppHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WhatsAppHubActivity extends AppCompatActivity {
    private ActivityWhatsappHubBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWhatsappHubBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
        loadData();
    }

    private void loadData() {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_RENT_RECORDS)
                .whereEqualTo("month", DateUtils.getCurrentMonth())
                .get()
                .addOnSuccessListener(snap -> {
                    List<RentRecord> list = new ArrayList<>();
                    int overdue = 0, pending = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        RentRecord r = doc.toObject(RentRecord.class);
                        if (Constants.STATUS_OVERDUE.equals(r.getStatus())) { list.add(r); overdue++; }
                        else if (Constants.STATUS_PENDING.equals(r.getStatus())) { list.add(r); pending++; }
                    }
                    binding.tvOverdueCount.setText(String.valueOf(overdue));
                    binding.tvPendingCount.setText(String.valueOf(pending));
                    binding.rvTenants.setLayoutManager(new LinearLayoutManager(this));
                    binding.rvTenants.setAdapter(new HubAdapter(list));
                });
    }

    class HubAdapter extends RecyclerView.Adapter<HubAdapter.VH> {
        private final List<RentRecord> list;
        HubAdapter(List<RentRecord> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(com.pgmanager.app.R.layout.item_whatsapp_tenant, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            RentRecord r = list.get(pos);
            h.tvAvatar.setText(r.getTenantName().substring(0, 1).toUpperCase(Locale.ENGLISH));
            h.tvName.setText(r.getTenantName());
            h.tvUnit.setText(r.getPgName());
            h.tvAmount.setText(CurrencyUtils.formatCurrency(r.getAmount()) + " • Due: " + DateUtils.formatTimestamp(r.getDueDate()));
            boolean isOverdue = Constants.STATUS_OVERDUE.equals(r.getStatus());
            h.tvStatus.setText(r.getStatus().toUpperCase(Locale.ENGLISH));
            h.tvStatus.setTextColor(getResources().getColor(isOverdue ? com.pgmanager.app.R.color.danger : com.pgmanager.app.R.color.warning));
            h.btnSend.setOnClickListener(v -> {
                String msg = isOverdue
                        ? WhatsAppHelper.getRentOverdueMessage(r.getTenantName(), r.getAmount(), r.getPgName())
                        : WhatsAppHelper.getRentReminderMessage(r.getTenantName(), r.getAmount(), r.getPgName());
                WhatsAppHelper.sendWhatsAppMessage(WhatsAppHubActivity.this, r.getWhatsappNumber(), msg);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvUnit, tvAmount, tvStatus;
            View btnSend;
            VH(View v) {
                super(v);
                tvAvatar = v.findViewById(com.pgmanager.app.R.id.tvAvatar);
                tvName = v.findViewById(com.pgmanager.app.R.id.tvName);
                tvUnit = v.findViewById(com.pgmanager.app.R.id.tvUnit);
                tvAmount = v.findViewById(com.pgmanager.app.R.id.tvAmount);
                tvStatus = v.findViewById(com.pgmanager.app.R.id.tvStatus);
                btnSend = v.findViewById(com.pgmanager.app.R.id.btnWhatsApp);
            }
        }
    }
}
