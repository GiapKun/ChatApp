package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.adapters.RecentConversationsAdapter;
import com.example.chatandcall_app.databinding.ActivityMainBinding;
import com.example.chatandcall_app.listeners.ConversionListener;
import com.example.chatandcall_app.models.ChatMessage;
import com.example.chatandcall_app.models.User;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.makeramen.roundedimageview.RoundedImageView;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
    SwitchCompat switchMode;
    boolean nightMode;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    //Enabled viewBinding, the binding class for each XML layout will be generated auto
    private ActivityMainBinding binding;
    private PreferenceManager preferecnceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private int currentPosition = RecyclerView.NO_POSITION;


    ImageButton buttonDrawerToggle;
    NavigationView navigationView ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferecnceManager = new PreferenceManager(getApplicationContext());
        buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);
        navigationView = findViewById(R.id.nav_menu);
        switchMode = findViewById(R.id.switchMode);
        init();
        loadUserDetails();
        setListeners();
        getToken();
        listenConversations();
    }



    private void init(){
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
        startService(preferecnceManager.getString(Constants.KEY_USER_ID),preferecnceManager.getString(Constants.KEY_NAME));
    }

    private  void setListeners() {
        binding.fabNewChat.setOnClickListener(v -> startActivity( new Intent(getApplicationContext(), UsersActivity.class)));
        buttonDrawerToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.drawerLayout.open();
            }
        });
        binding.navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId= item.getItemId();

                if (itemId == R.id.nav_home){
                    showToast("This Home");
                }
                if (itemId == R.id.nav_info){
                    showToast("This Information");

                }
                if (itemId == R.id.nav_setting){
                    startActivity( new Intent(getApplicationContext(), ChangePasswordActivity.class));
                }
                if (itemId == R.id.nav_signOut){
                    signOut();
                }

                binding.drawerLayout.close();
                return false;
            }
        });

        sharedPreferences = getSharedPreferences("Mode", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);
        if(nightMode)
        {
            switchMode.setChecked(true);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nightMode)
                {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", false);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", true);
                }
                editor.apply();
            }
        });
    }

    //Load thông tin
    private void loadUserDetails(){
        View headerView = navigationView.getHeaderView(0);
        TextView textName = headerView.findViewById(R.id.textName);
        RoundedImageView roundedImageView = headerView.findViewById(R.id.imageProfile);
        textName.setText(preferecnceManager.getString(Constants.KEY_NAME));
        //Giải mã dữ liệu hình ảnh từ chuỗi Base64.
        byte[] bytes = Base64.decode(preferecnceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        // Chuyển đổi mảng byte thành đối tượng Bitmap.
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        //Hiển thị hình ảnh người dùng lên ImageView
        roundedImageView.setImageBitmap(bitmap);
    }

    private  void showToast(String message){
        Toast.makeText(getApplicationContext(), message,Toast.LENGTH_SHORT).show();
    }

    //Lấy token trên máy
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private  void updateToken(String token){
        preferecnceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(preferecnceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut(){
        showToast("Signing out...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(preferecnceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferecnceManager.clear();
                    startActivity( new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
    
    //Set up call video
    private void startService(String senderID,String senderName) {
        Application application = getApplication(); // Android's application context
        long appID = 968668767;   // yourAppID
        String appSign ="7f143ce516bbacf0ffa0c8cf65e552a0cde048208836623acf0ad2f66cf6bcb4";  // yourAppSign
//        String userName = userID; // yourUserID, userID should only contain numbers, English characters, and '_'.
        String userID = senderID; // yourUserID, userID should only contain numbers, English characters, and '_'.
        String userName = senderName;   // yourUserName

        ZegoUIKitPrebuiltCallInvitationConfig callInvitationConfig = new ZegoUIKitPrebuiltCallInvitationConfig();
        ZegoUIKitPrebuiltCallInvitationService.init(getApplication(), appID, appSign, userID, userName,callInvitationConfig);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoUIKitPrebuiltCallInvitationService.unInit();
    }


    //Event click Conversion
    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }

    //Event hold conversion
    public void onConversionHold(User user) {
    binding.conversationsRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { // Kiểm tra khi nào người dùng bắt đầu hold
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null) {
                    currentPosition = rv.getChildAdapterPosition(childView);
                    showPopupMenu(user);
                }
            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    });
}

    private void showPopupMenu(User user) {
        if (currentPosition >= 0) {
            PopupMenu popupMenu = new PopupMenu(getApplicationContext(), binding.conversationsRecyclerView.getChildAt(currentPosition));
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.item1) {
                        deleteConversation(user.id, preferecnceManager.getString(Constants.KEY_USER_ID));
                        deleteConversation(preferecnceManager.getString(Constants.KEY_USER_ID), user.id);
                    }
                    return true;
                }
            });
            popupMenu.show();
        }
    }



    // Xóa trò chuyện và xóa chat
    public void deleteConversation(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null){
                            for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                // Get the document ID
                                String documentId = queryDocumentSnapshot.getId();

                                // Delete the document
                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                        .document(documentId)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Sau khi xóa conversation, cập nhật giao diện:
                                                conversations.removeIf(conversation -> conversation.senderId.equals(senderId) && conversation.receiverId.equals(receiverId));
                                                conversationsAdapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w("Results", "Error deleting conservation", e);
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    public void deleteChats(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                // Get the document ID
                                String documentId = queryDocumentSnapshot.getId();

                                // Delete the document
                                database.collection(Constants.KEY_COLLECTION_CHAT)
                                        .document(documentId)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("Results", "Chat deleted successfully");
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

                    }
                });
    }


    // Lắng nghe các sự thay đổi về tin nhắn với, và cuộc trò chuyện mới
    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferecnceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;

                    if (preferecnceManager.getString(Constants.KEY_USER_ID).equals(senderId))   {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    }
                    else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }
                else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }

            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

}