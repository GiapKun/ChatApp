package com.example.chatandcall_app.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;

import com.example.chatandcall_app.adapters.UsersAdapter;
import com.example.chatandcall_app.databinding.ActivityUsersBinding;
import com.example.chatandcall_app.listeners.UserListener;
import com.example.chatandcall_app.models.User;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

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
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }


        });
    }


    private void performSearch(String query) {
        if (query.isEmpty()){
            getUsers();
            binding.textErrorMessage.setText("");
            binding.textErrorMessage.setVisibility(View.GONE);
        }
        else{
            loading(true);
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .get()
                    .addOnCompleteListener(task -> {
                        loading(false);
                        String currentUserId = preferecnceManager.getString(Constants.KEY_USER_ID);
                        if (task.isSuccessful() && task.getResult() != null){
                            List<User> users = new ArrayList<>();
                            for(QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
                                if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                    continue;
                                }
                                if(queryDocumentSnapshot.getString(Constants.KEY_NAME).toLowerCase().contains(query.toLowerCase())){
                                    User user = new User();
                                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                    user.id = queryDocumentSnapshot.getId();
                                    users.add(user);
                                }

                            }
                            if(users.size() > 0){
                                UsersAdapter usersAdapter =  new UsersAdapter(users, this);
                                binding.txtSuggest.setVisibility(View.GONE);
                                binding.usersRecyclerView.setAdapter(usersAdapter);
                                binding.usersRecyclerView.setVisibility(View.VISIBLE);
                            }
                            else {
                                binding.txtSuggest.setVisibility(View.GONE);
                                binding.textErrorMessage.setText(String.format("%s", "No Result"));
                                binding.textErrorMessage.setVisibility(View.VISIBLE);
                                binding.usersRecyclerView.setVisibility(View.GONE);
                            }
                        }
                    });
        }
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
                            binding.txtSuggest.setVisibility(View.VISIBLE);
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
    }
}