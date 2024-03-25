package com.example.chatandcall_app.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.activities.ShowImageActivity;
import com.example.chatandcall_app.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatandcall_app.databinding.ItemContainerSentMessageBinding;
import com.example.chatandcall_app.listeners.ChatListener;
import com.example.chatandcall_app.models.ChatMessage;
import com.example.chatandcall_app.utilities.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;

    private ChatListener chatListener;
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    //Constructor
    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, ChatListener chatListener) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.chatListener = chatListener;
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


    class SentMessageViewHolder extends RecyclerView.ViewHolder {
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
                binding.image.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
            }).addOnFailureListener(exception -> {
                // Hình ảnh không tồn tại trong bộ lưu trữ Storage
                binding.textMessage.setText(chatMessage.message);
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
            });

            binding.getRoot().setOnClickListener(v -> {
                ChatMessage message = new ChatMessage();
                message.senderId = chatMessage.senderId;
                message.receiverId =chatMessage.receiverId;
                message.message = chatMessage.message;
                message.dateTime = chatMessage.dateTime;
                message.dateObject = chatMessage.dateObject;

                StorageReference storageRef1 = FirebaseStorage.getInstance().getReference();
                StorageReference imageRef1 = storageRef1.child("images/" + chatMessage.message);
                imageRef1.getDownloadUrl().addOnSuccessListener(uri -> {
//            // Hình ảnh tồn tại trong bộ lưu trữ Storage
                    Intent intent = new Intent(v.getContext(), ShowImageActivity.class);
                    intent.putExtra("uri", uri.toString());
                    v.getContext().startActivity(intent);

                }).addOnFailureListener(exception -> {
                    // Hình ảnh không tồn tại trong bộ lưu trữ Storage
                    showPopupMenu(v,message);
                });
            });
        }
        private void showPopupMenu(View view,ChatMessage message) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.popup_menu);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.item1) {
                        int adapterPosition = getAdapterPosition();
                        deleteMessage(message.message,message.receiverId,message.senderId,message.dateObject, adapterPosition);
                    }
                    return true;
                }
            });
            popupMenu.show();
        }

        private void deleteMessage(String mess, String rec, String send, Date datetime,Integer position) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            Query query = firestore.collection(Constants.KEY_COLLECTION_CHAT)
                    .whereEqualTo("message", mess)
                    .whereEqualTo("receiverId",rec)
                    .whereEqualTo("senderId",send)
                    .whereEqualTo("timestamp",datetime);
            query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        // Lấy id của người dùng
                        String userId = documentSnapshot.getId();
                        // Delete the document
                        firestore.collection(Constants.KEY_COLLECTION_CHAT)
                                .document(userId)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        chatListener.removeItem(position);
                                        Log.d("test","Success");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Results", "Error deleting chat", e);
                                    }
                                });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@androidx.annotation.NonNull Exception e) {
                    Log.e("Error", "Error getting user documents: " + e.getMessage());
                }
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

            binding.getRoot().setOnClickListener(v -> {
                ChatMessage message = new ChatMessage();
                message.senderId = chatMessage.senderId;
                message.receiverId =chatMessage.receiverId;
                message.message = chatMessage.message;
                message.dateTime = chatMessage.dateTime;
                message.dateObject = chatMessage.dateObject;

                StorageReference storageRef1 = FirebaseStorage.getInstance().getReference();
                StorageReference imageRef1 = storageRef1.child("images/" + chatMessage.message);
                imageRef1.getDownloadUrl().addOnSuccessListener(uri -> {
//            // Hình ảnh tồn tại trong bộ lưu trữ Storage
                    Intent intent = new Intent(v.getContext(), ShowImageActivity.class);
                    intent.putExtra("uri", uri.toString());
                    v.getContext().startActivity(intent);

                }).addOnFailureListener(exception -> {
                    // Hình ảnh không tồn tại trong bộ lưu trữ Storage

                });
            });
        }

    }
}
