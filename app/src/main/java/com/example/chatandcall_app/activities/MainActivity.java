package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivityMainBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    //Enabled viewBinding, the binding class for each XML layout will be generated auto
    private ActivityMainBinding binding;
    private PreferenceManager preferecnceManager;


    ImageButton buttonDrawerToggle;
    RoundedImageView roundedImageView;
    NavigationView navigationView ;
    TextView textName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferecnceManager = new PreferenceManager(getApplicationContext());
        buttonDrawerToggle = findViewById(R.id.buttonDrawerToggle);
        loadUserDetails();
        setListeners();
        getToken();
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
                    showToast("This Setting");
                }
                if (itemId == R.id.nav_signOut){
                    signOut();
                }

                binding.drawerLayout.close();
                return false;
            }
        });
    }

    private void loadUserDetails(){
        navigationView = findViewById(R.id.nav_menu);
        View headerView = navigationView.getHeaderView(0);
        textName = headerView.findViewById(R.id.textName);
        roundedImageView = headerView.findViewById(R.id.imageProfile);
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

}