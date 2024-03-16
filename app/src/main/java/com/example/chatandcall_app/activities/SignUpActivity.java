package com.example.chatandcall_app.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatandcall_app.databinding.ActivitySignUpBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private  String encodedImage;
    private int  otp ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySignUpBinding.inflate(getLayoutInflater());
        preferenceManager =new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        setListeners();
    }
    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()){
                String receiverEmail = binding.inputEmail.getText().toString().trim();
                otp = (int) (Math.random() * 900000) + 100000;
                sendEmail(otp,receiverEmail);
                showDialog();
            }
        });
        //Select img
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showDialog(){
        // Create new Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
        builder.setTitle("Nhập OTP");

        // Tạo một layout để chứa EditText và TextView đếm ngược
        LinearLayout layout = new LinearLayout(SignUpActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Tạo một trường nhập dữ liệu trong hộp thoại dialog
        final EditText input = new EditText(SignUpActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(input);

        // Tạo một TextView để hiển thị đếm ngược
        final TextView countDownTextView = new TextView(SignUpActivity.this);
        // Đặt khoảng cách 2dp cho TextView countDownTextView
        countDownTextView.setPadding(10, 10, 0, 0); // left, top, right, bottom
        layout.addView(countDownTextView);

        builder.setView(layout);

        // Bộ đếm ngược
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                countDownTextView.setText(seconds + "s");
            }

            public void onFinish() {
                // Khi bộ đếm kết thúc
                countDownTextView.setText("");
            }
        }.start();

        builder.setPositiveButton("Xác nhận", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputOTP = input.getText().toString();
                if (!countDownTextView.getText().toString().isEmpty()){
                    if (Integer.valueOf(inputOTP) == otp) {
                        signUp();
                    } else {
                        init();
                        showToast("OTP is incorrect, please enter again");
                    }
                }
                else {
                    dialog.cancel();
                    init();
                    showToast("OTP expired, please enter again");
                }
            }
        });

        builder.setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void init(){
        binding.imageProfile.setImageBitmap(null);
        binding.textAddImage.setVisibility(View.VISIBLE);
        binding.inputName.setText("");
        binding.inputEmail.setText("");
        binding.inputPassword.setText("");
        binding.inputConfirmPassword.setText("");
    }
    private void sendEmail(Integer otp, String receiverEmail) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", Constants.Gmail_Host);
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constants.Sender_Email_Address, Constants.Sender_Email_Password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiverEmail));
            message.setSubject("Xác thực tài khoản với mã OTP");

            // Định dạng nội dung email
            String emailContent = "Chào bạn,\n\n"
                    + "Bạn đã yêu cầu xác thực tài khoản của mình. Đây là mã OTP của bạn:\n\n"
                    + "Mã OTP: " + otp + "\n\n"
                    + "Vui lòng nhập mã này vào trang web hoặc ứng dụng của chúng tôi để hoàn tất quy trình xác thực.\n\n"
                    + "Lưu ý: Mã OTP này chỉ có hiệu lực trong vòng 1 phút kể từ khi bạn nhận được email này.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Nếu bạn vẫn gặp vấn đề hoặc cần sự trợ giúp, đừng ngần ngại liên hệ với chúng tôi.\n\n"
                    + "Trân trọng,\n"
                    + "Chat App Huflit";

            // Thiết lập nội dung của email
            message.setText(emailContent);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Transport.send(message);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void signUp() {
        loading(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME,binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL,binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE,encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
//                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
//                    preferenceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
//                    preferenceManager.putString(Constants.KEY_NAME,binding.inputName.getText().toString());
//                    preferenceManager.putString(Constants.KEY_IMAGE,encodedImage);
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "Successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }


    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    //Kiểm tra tính hợp lệ của thông tin
    private Boolean isValidSignUpDetails(){
        if(encodedImage == null){
            showToast("Select profile img");
            return false;
        }
        else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Enter name");
            return false;
        }
        else if(binding.inputEmail.getText().toString().trim().isEmpty()){
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
        else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("Confirm your password");
            return false;
        }
        else if(!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            showToast("Password and Confirm Password must be same");
            return false;
        }
        else {
            return  true;
        }
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

    //mã hóa một hình ảnh thành một chuỗi Base64
    private String encodeImage(Bitmap bitmap){
        int previewWidth=150;
        int previewHeight= bitmap.getHeight() * previewWidth/bitmap.getWidth();
        Bitmap previewBitmap =Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

//    chọn ảnh trong ứng dụng Android sử dụng API Activity Result
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    if(result.getData() != null){
                        Uri imgUri=result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imgUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        }
                        catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
}