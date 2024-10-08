package com.example.chatandcall_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivitySignInBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {


    private ActivitySignInBinding binding;
    private PreferenceManager preferecnceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_sign_in);
        setContentView(binding.getRoot());

        preferecnceManager = new PreferenceManager(getApplicationContext());
        //Auto SignIn
        if(preferecnceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        setListeners();
    }
    private  void setListeners(){
        binding.textCreateNewAccount.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.textForgotPassword.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), Send_OTP_Activity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if(isValidSignInDetails()){
                signIn();
            }
        });
        binding.buttonTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý sự kiện khi người dùng nhấn vào Button để chuyển đổi giữa hiển thị và ẩn password
                if (binding.inputPassword.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Nếu đang hiển thị password, chuyển sang chế độ ẩn password
                    binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    // Thiết lập biểu tượng cho Button để hiển thị biểu tượng ẩn password
                    binding.buttonTogglePassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                } else {
                    // Nếu đang ẩn password, chuyển sang chế độ hiển thị password
                    binding.inputPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    // Thiết lập biểu tượng cho Button để hiển thị biểu tượng hiển thị password
                    binding.buttonTogglePassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eyeoff, 0);
                }
            }
        });
    }
    private void signIn(){
        loading(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL,binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferecnceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                        preferecnceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId());
                        preferecnceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                        preferecnceManager.putString(Constants.KEY_PASSWORD,documentSnapshot.getString(Constants.KEY_PASSWORD));
                        preferecnceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    else{
                        loading(false);
                        showToast("Unable to sign in");
                    }
                });
    }

    //Loading
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    //Kiểm tra tính hợp lệ của thông tin đăng nhập nhập
    private Boolean isValidSignInDetails(){
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter email");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid email");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter password");
            return false;
        }

        else {
            return  true;
        }
    }
}