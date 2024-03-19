package com.example.chatandcall_app.utilities;

import java.util.HashMap;

public class Constants {
    public static final String ChatGPT_ID = "WNjBJEEy6TnsGcqpnHXP";
    public static final String Sender_Email_Address ="pettertran1121@gmail.com";
    public static final String Sender_Email_Password ="osrz ukpf adry gxpg";
    public static final String Gmail_Host ="smtp.gmail.com";
    public static final String KEY_COLLECTION_USERS ="users";
    public static final String KEY_NAME ="name";
    public static final String KEY_EMAIL ="gmail";
    public static final String KEY_PASSWORD ="password";
    public static final String KEY_PREFERENCE_NAME ="chatAppPreference";
    public static final String KEY_IS_SIGNED_IN ="isSignedIn";
    public static final String KEY_USER_ID ="userId";
    public static final String KEY_IMAGE ="image";
    public static final String KEY_FCM_TOKEN ="fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT ="chat";
    public static final String KEY_SENDER_ID ="senderId";
    public static final String KEY_RECEIVER_ID ="receiverId";
    public static final String KEY_MESSAGE ="message";
    public static final String KEY_TIMESTAMP ="timestamp";

    public static final String KEY_COLLECTION_CONVERSATIONS ="conversations";
    public static final String KEY_SENDER_NAME ="senderName";
    public static final String KEY_RECEIVER_NAME ="receiverName";
    public static final String KEY_SENDER_IMAGE ="senderImage";
    public static final String KEY_RECEIVER_IMAGE ="receiverImage";
    public static final String KEY_LAST_MESSAGE ="lastMessage";

    public static final String KEY_AVAILABILITY ="availability";

    public static final String REMOTE_MSG_AUTHORIZATION ="Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE ="Content-Type";
    public static final String REMOTE_MSG_DATA ="data";
    public static final String REMOTE_MSG_REGISTRATION_IDS ="registration_ids";

    public static HashMap<String, String> remoteMsgHeaders = null;
    public static HashMap<String, String> getRemoteMsgHeaders(){
        if( remoteMsgHeaders == null){
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=AAAALYF5W3A:APA91bHM1_U_P-0O_QFUL8xkmWqy8fJIa2FzhNWw9dv9q4gs0KtQ2-Sjx3-oNcYa_E2PP7UQwSjywMi7Lxn-P7akNEWreOTUCQlztwY-B233aF0ylw6RFixxJVlbTcwUuWZzKuLWzbVh"
            );
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
