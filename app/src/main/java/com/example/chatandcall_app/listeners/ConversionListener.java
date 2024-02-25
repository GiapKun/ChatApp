package com.example.chatandcall_app.listeners;

import com.example.chatandcall_app.models.User;

public interface ConversionListener {
    void onConversionClicked(User user);
    void onConversionHold(User user);
}
