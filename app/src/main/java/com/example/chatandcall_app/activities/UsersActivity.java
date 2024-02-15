package com.example.chatandcall_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.chatandcall_app.R;
import com.example.chatandcall_app.adapters.UsersAdapter;
import com.example.chatandcall_app.databinding.ActivityUsersBinding;
import com.example.chatandcall_app.listeners.UserListener;
import com.example.chatandcall_app.models.User;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.ref.PhantomReference;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferecnceManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferecnceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }

    private  void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());

    }

    private  void getUsers(){
        loading(true);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferecnceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult() != null ){
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if(users.size() > 0){
                            UsersAdapter usersAdapter =  new UsersAdapter(users, this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        }
                        else{
                            showErrorMessage();
                        }
                    }
                    else {
                        showErrorMessage();
                    }
                });
    }


    //Loading
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class );
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
//        finish();
    }
}