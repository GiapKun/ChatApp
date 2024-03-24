package com.example.chatandcall_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivitySendOtpBinding;
import com.example.chatandcall_app.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Send_OTP_Activity extends AppCompatActivity {

    private ActivitySendOtpBinding binding;
    private int  otp ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySendOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
    }
    private void init(){
        binding.inputEmail.setText("");
    }
    private void setListener(){
        binding.buttonGetOTP.setOnClickListener(v->{
            if (isValidEmailDetails()){
                loading(true);
                String receiverEmail = binding.inputEmail.getText().toString().trim();
                checkAvailable(receiverEmail, new OnCheckAvailableListener() {
                    @Override
                    public void onCheckAvailable(boolean isAvailable) {
                        if (isAvailable) {
                            // Email đã tồn tại
                            otp = (int) (Math.random() * 900000) + 100000;
                            sendEmail(otp,receiverEmail);
                            loading(false);
                            init();
                            startVerifyOTPActivity(otp,receiverEmail);
                        } else {
                            // Email chưa tồn tại
                            init();
                            showToast("Email doesn't exists. Please try another email!");
                            loading(false);
                        }
                    }
                });
            }
        });
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void startVerifyOTPActivity(int otp, String receiverEmail) {
        Intent intent = new Intent(getApplicationContext(), VerifyOTPActivity.class);
        intent.putExtra("OTP", otp);
        intent.putExtra("email", receiverEmail);
        startActivity(intent);
    }


    //Kiem tra email co ton tai ben trong database khong
    private void checkAvailable(String email, Send_OTP_Activity.OnCheckAvailableListener listener) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isExisted = !task.getResult().isEmpty();
                        listener.onCheckAvailable(isExisted);
                    } else {
                        listener.onCheckAvailable(false);
                    }
                });
    }

    // Định nghĩa một interface để sử dụng callback
    interface OnCheckAvailableListener {
        void onCheckAvailable(boolean isAvailable);
    }

    //Kiem tra dinh dang email
    private Boolean isValidEmailDetails(){
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter email");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid email");
            return false;
        }

        else {
            return  true;
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonGetOTP.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonGetOTP.setVisibility(View.VISIBLE);
        }
    }

    //Mail xac thuc OTP
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
}