package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatandcall_app.databinding.ActivityChangePasswordBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferecnceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();

    }

    private void init(){
        preferecnceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
    }
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.buttonChangePassword.setOnClickListener(v -> {
           if (isValidChangePasswordDetails()){
               updatePassword(binding.inputNewPassword.getText().toString().trim());
           }
        });
    }

    private void updatePassword(String newPassword) {
        loading(true);
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferecnceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_PASSWORD, newPassword); // Cập nhật trường password
        documentReference.update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        signOut();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Test","Fail");
                    }
                });
    }
    private Boolean isValidChangePasswordDetails(){
        if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Please enter your current password");
            return false;
        }
//        Check inputpassword
        else if (!binding.inputPassword.getText().toString().equals(preferecnceManager.getString(Constants.KEY_PASSWORD))) {
            showToast("Incorrect password");
            return false;
        }
        else if(binding.inputNewPassword.getText().toString().trim().isEmpty()){
            showToast("Please enter your current new password");
            return false;
        }

        else if(binding.inputConfirmNewPassword.getText().toString().trim().isEmpty()){
            showToast("Please confirm your new password");
            return false;
        }
        else if(!binding.inputNewPassword.getText().toString().equals(binding.inputConfirmNewPassword.getText().toString())){
            showToast("New password and confirm new password must be same");
            return false;
        }
        else if (binding.inputNewPassword.getText().toString().equals(preferecnceManager.getString(Constants.KEY_PASSWORD))) {
            showToast("The current password and new password must not be the same");
            return false;
        }
        else {
            return  true;
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonChangePassword.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonChangePassword.setVisibility(View.VISIBLE);
        }
    }

    private void signOut(){
        loading(false);
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
                    Toast.makeText(getApplicationContext(), "Successfully, Please log in again!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}