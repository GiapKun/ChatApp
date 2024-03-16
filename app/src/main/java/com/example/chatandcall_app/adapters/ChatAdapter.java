package com.example.chatandcall_app.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandcall_app.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatandcall_app.databinding.ItemContainerSentMessageBinding;
import com.example.chatandcall_app.models.ChatMessage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    //Constructor
    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT){
            return  new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent,
                            false));
        }
        else {
            return  new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent,
                            false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }
        else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position),receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }



    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)){
            return  VIEW_TYPE_SENT;
        }
        else{
            return  VIEW_TYPE_RECEIVED;
        }
    }



    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage = bitmap;
    }


    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            String imageName = chatMessage.message;
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference imageRef = storageRef.child("images/" + imageName);

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Hình ảnh tồn tại trong bộ lưu trữ Storage
                Picasso.get().load(uri).into(binding.image);
                binding.image.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
            }).addOnFailureListener(exception -> {
                // Hình ảnh không tồn tại trong bộ lưu trữ Storage
                binding.textMessage.setText(chatMessage.message);
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
            });

        }
    }

    static  class  ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerRecievedMessageBinding){
            super(itemContainerRecievedMessageBinding.getRoot());
            binding = itemContainerRecievedMessageBinding;
        }
        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage){
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null){
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
            String imageName = chatMessage.message;
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference imageRef = storageRef.child("images/" + imageName);

            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Hình ảnh tồn tại trong bộ lưu trữ Storage
                Picasso.get().load(uri).into(binding.image);
                binding.image.setVisibility(View.VISIBLE);
            }).addOnFailureListener(exception -> {
                // Hình ảnh không tồn tại trong bộ lưu trữ Storage
                binding.textMessage.setText(chatMessage.message);
                binding.textMessage.setVisibility(View.VISIBLE);
            });
        }

    }
}
