package com.example.chatandcall_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivityVerifyOtpactivityBinding;
import com.example.chatandcall_app.utilities.Constants;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class VerifyOTPActivity extends AppCompatActivity {

    private ActivityVerifyOtpactivityBinding binding;
    private int OTP;
    private String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        Intent intent = getIntent();
        if (intent != null) {
            OTP = intent.getIntExtra("OTP", 0);
            email = intent.getStringExtra("email");
        }
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.buttonVerify.setOnClickListener(v -> {
            String inputCode1 = binding.inputCode1.getText().toString();
            String inputCode2 = binding.inputCode2.getText().toString().trim();
            String inputCode3 = binding.inputCode3.getText().toString().trim();
            String inputCode4 = binding.inputCode4.getText().toString().trim();
            String inputCode5 = binding.inputCode5.getText().toString().trim();
            String inputCode6 = binding.inputCode6.getText().toString().trim();
            String code = inputCode1 + inputCode2 + inputCode3 + inputCode4 + inputCode5 +inputCode6 ;
           if (isValidOTPDetails(inputCode1,inputCode2,inputCode3,inputCode4,inputCode5,inputCode6)){
               if (code.equals(String.valueOf(OTP))){
                   //True
                   init();
                   startForgotPasswordActivity(email);
               }
               else {
                   showToast("OTP is wrong");
                   init();
               }
           }
        });
        binding.textResendOTP.setOnClickListener(v -> {
            OTP = (int) (Math.random() * 900000) + 100000;
            sendEmail(OTP,email);
            showToast("OTP has been sent");
            init();
        });

    }
    private void startForgotPasswordActivity(String receiverEmail) {
        Intent intent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
        intent.putExtra("email", receiverEmail);
        startActivity(intent);
        finish();
    }

    private void init(){
        binding.inputCode1.setText("");
        binding.inputCode2.setText("");
        binding.inputCode3.setText("");
        binding.inputCode4.setText("");
        binding.inputCode5.setText("");
        binding.inputCode6.setText("");
    }
    private Boolean isValidOTPDetails(String a, String b, String c, String d, String e, String f){
        if (a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || e.isEmpty()
                || f.isEmpty() ){
            showToast("Please enter valid code");
            return  false;
        }
        else {
            return  true;
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    //Nhận OTP
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