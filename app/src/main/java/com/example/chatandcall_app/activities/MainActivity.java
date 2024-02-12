package com.example.chatandcall_app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    //Enabled viewBinding, the binding class for each XML layout will be generated auto
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //đối tượng binding sẽ giữ thông tin về tất cả các thành phần UI
        binding= ActivityMainBinding.inflate(getLayoutInflater());
        //setContentView sẽ hiển thị chúng trên màn hình
        setContentView(binding.getRoot());

    }

}