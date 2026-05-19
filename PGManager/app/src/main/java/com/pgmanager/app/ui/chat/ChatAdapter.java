package com.pgmanager.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pgmanager.app.data.model.ChatMessage;
import com.pgmanager.app.databinding.ItemChatMessageBinding;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        ChatViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage msg) {
            if (msg.getType() == ChatMessage.TYPE_BOT) {
                binding.layoutBot.setVisibility(View.VISIBLE);
                binding.layoutUser.setVisibility(View.GONE);
                binding.tvBotMessage.setText(msg.getText());
            } else {
                binding.layoutUser.setVisibility(View.VISIBLE);
                binding.layoutBot.setVisibility(View.GONE);
                binding.tvUserMessage.setText(msg.getText());
            }
        }
    }
}
