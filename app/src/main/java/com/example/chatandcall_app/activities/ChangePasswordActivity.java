package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthCredential;

public class ChangePasswordActivity extends AppCompatActivity {

    private FirebaseAuth authProfile;
    private EditText currentPassword,newPassword, confirmPassword;
    private Button btnChangePassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        currentPassword = findViewById(R.id.currentPassword);
        newPassword = findViewById(R.id.newPassword);
        confirmPassword = findViewById(R.id.newPassword1);
        progressBar = findViewById(R.id.progressBar);
        btnChangePassword = findViewById(R.id.btnChangePassword);



        authProfile= FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();

            btnChangePassword.setOnClickListener(new View.OnClickListener() {
                String userPwdCurr = currentPassword.getText().toString();
                String newPwd = newPassword.getText().toString();
                String confirmPwd = confirmPassword.getText().toString();
                @Override
                public void onClick(View v) {
                    if (newPwd.length() > 0)
                    {
                        newPassword.setError("Field can't be empty");
                    }
                    else if(confirmPwd.length()>0)
                    {
                        confirmPassword.setError("Field can't be empty");
                    }
                    else if(newPwd.compareTo(confirmPwd) != 0)
                    {
                        Toast.makeText(ChangePasswordActivity.this, "Password and Confirm Password should be same", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        updatePassword(userPwdCurr, newPwd);
                    }
                }
            });



    }

    public void updatePassword(String userPwdCurr, String newPwd) {
        // [START update_password]
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), userPwdCurr);

        user.reauthenticate(authCredential).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                user.updatePassword(newPwd).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(ChangePasswordActivity.this, "Password Updated ", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChangePasswordActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChangePasswordActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }



}