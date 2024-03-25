package com.example.chatandcall_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.chatandcall_app.databinding.ActivityShowImageBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Random;

public class ShowImageActivity extends AppCompatActivity {


    private Uri uri;
    private ActivityShowImageBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShowImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadImage();
        setListeners();
    }

    private void loadImage() {
        Intent intent = getIntent();
        if (intent != null) {
            uri = Uri.parse(intent.getStringExtra("uri"));
        }
        Picasso.get().load(uri).into(binding.image);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.imageDownLoad.setOnClickListener(v -> {
            String randomString = generateRandomString(10);
            downloadImageNew(randomString,uri.toString());
        });
    }

    private void downloadImageNew(String filename, String downloadUrlOfImage) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType("image/jpeg") // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename + ".jpg");
            dm.enqueue(request);

            showToast("Image download started.");
        } catch (Exception e) {
            showToast("Image download failed.");
        }
    }
    private  void showToast(String message){
        Toast.makeText(getApplicationContext(), message,Toast.LENGTH_SHORT).show();
    }

    private String generateRandomString(int length) {
        // Khởi tạo một đối tượng Random
        Random random = new Random();

        // Khởi tạo một mảng char để chứa ký tự ngẫu nhiên
        char[] chars = new char[length];

        // Khởi tạo các ký tự ngẫu nhiên
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ('a' + random.nextInt(26)); // Số ngẫu nhiên từ 0 đến 25, tương ứng với ký tự 'a' đến 'z'
        }

        // Chuyển đổi mảng char thành chuỗi
        return new String(chars);
    }

}