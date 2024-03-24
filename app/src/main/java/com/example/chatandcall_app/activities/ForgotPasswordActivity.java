package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivityForgotPasswordBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        if (intent != null) {
            email = intent.getStringExtra("email");
        }
        setOnListeners();
    }

    private void setOnListeners(){
        binding.imageBack.setOnClickListener(v-> {
            startActivity(new Intent(getApplicationContext(), Send_OTP_Activity.class));
            finish();
        });
        binding.buttonToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(binding.buttonToggleNewPassword, binding.inputNewPassword));
        binding.buttonToggleConfirmNewPassword.setOnClickListener(v -> togglePasswordVisibility(binding.buttonToggleConfirmNewPassword, binding.inputConfirmNewPassword));
        binding.buttonChangePassword.setOnClickListener(v -> {
            loading(true);
            if (isValidChangePasswordDetails()){
                updatePassword(binding.inputNewPassword.getText().toString().trim(),email);
            }

        });
    }

    private void updatePassword(String newPassword, String yourEmail) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Query query = firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("gmail", yourEmail);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_PASSWORD, newPassword);
        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    // Lấy id của người dùng
                    String userId = documentSnapshot.getId();

//                    // Thực hiện cập nhật
                      firestore.collection(Constants.KEY_COLLECTION_USERS)
                            .document(userId)
                            .update(updates)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    loading(false);
                                    startActivity( new Intent(getApplicationContext(), SignInActivity.class));
                                    finish();
                                    Toast.makeText(getApplicationContext(), "Successfully!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    loading(false);
                                    startActivity( new Intent(getApplicationContext(), SignInActivity.class));
                                    finish();
                                    Toast.makeText(getApplicationContext(), "Password is the same as the old password.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Error", "Error getting user documents: " + e.getMessage());
            }
        });
    }

    private void togglePasswordVisibility(Button button, EditText editText) {
        // Xử lý sự kiện khi người dùng nhấn vào Button để chuyển đổi giữa hiển thị và ẩn password
        if (editText.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Nếu đang hiển thị password, chuyển sang chế độ ẩn password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            // Thiết lập biểu tượng cho Button để hiển thị biểu tượng ẩn password
            button.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        } else {
            // Nếu đang ẩn password, chuyển sang chế độ hiển thị password
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            // Thiết lập biểu tượng cho Button để hiển thị biểu tượng hiển thị password
            button.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eyeoff, 0);
        }
    }
    private Boolean isValidChangePasswordDetails(){

         if(binding.inputNewPassword.getText().toString().trim().isEmpty()){
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
}