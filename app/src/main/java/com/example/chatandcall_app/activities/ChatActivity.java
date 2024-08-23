package com.example.chatandcall_app.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.chatandcall_app.adapters.ChatAdapter;
import com.example.chatandcall_app.databinding.ActivityChatBinding;
import com.example.chatandcall_app.listeners.ChatListener;
import com.example.chatandcall_app.models.ChatMessage;
import com.example.chatandcall_app.models.User;
import com.example.chatandcall_app.network.ApiClient;
import com.example.chatandcall_app.network.ApiService;
import com.example.chatandcall_app.utilities.Constants;
import com.example.chatandcall_app.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.zegocloud.uikit.plugin.invitation.ZegoInvitationType;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment;
import com.zegocloud.uikit.prebuilt.call.config.DurationUpdateListener;
import com.zegocloud.uikit.prebuilt.call.config.ZegoCallDurationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallInvitationData;
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoUIKitPrebuiltCallConfigProvider;
import com.zegocloud.uikit.service.defines.ZegoUIKitUser;


import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity implements ChatListener {
    
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferecnceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    private final int PICK_IMAGE_CAMERA = 1, PICK_IMAGE_GALLERY = 2;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
        // Đặt mã yêu cầu quyền SYSTEM_ALERT_WINDOW
        PermissionX.init(this)
                .permissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                .onExplainRequestReason(new ExplainReasonCallback() {
                    @Override
                    public void onExplainReason(@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
                        String message = "We need your consent for the following permissions in order to use the offline call function properly";
                        scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny");
                    }
                }).request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, @NonNull List<String> grantedList,
                                         @NonNull List<String> deniedList) {
                        // Xử lý kết quả của yêu cầu quyền ở đây
                    }
                });
    }

    private void init(){
        preferecnceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferecnceManager.getString(Constants.KEY_USER_ID),
                this
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
        startService(preferecnceManager.getString(Constants.KEY_USER_ID),preferecnceManager.getString(Constants.KEY_NAME));
    }

    private Bitmap getBitmapFromEncodedString(String encodedImage){
        if (encodedImage != null){
            byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        }
        else {
            return null;
        }
    }

    private String getReadableDateTime(Date date)   {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private  void showToast(String message){
        Toast.makeText(getApplicationContext(), message,Toast.LENGTH_SHORT).show();
    }

    //Lấy thông tin ng nhận từ intent và setup video call
    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
        if (receiverUser.id.equals(Constants.ChatGPT_ID)){
            binding.layoutPicture.setVisibility(View.GONE);
        }
        else {
            setVoiceCall(receiverUser.id,receiverUser.name);
            setVideoCall(receiverUser.id,receiverUser.name);
        }
    }


    // Các sự kiện click
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            if (binding.inputMessage.getText().toString().isEmpty())
            {
                showToast("Please fill in the content");
            }
            else {
                if (!preferecnceManager.getString(Constants.KEY_USER_ID).equals(Constants.ChatGPT_ID) && receiverUser.id.equals(Constants.ChatGPT_ID)) {
                        String question =binding.inputMessage.getText().toString();
                        GPTRequest(question);
                        callApiInBackground(question);
                }
                else {
                        sendMessage(binding.inputMessage.getText().toString().trim());
                }
            }
        });
        binding.layoutPicture.setOnClickListener(v -> selectImage());
    }


    @Override
    public void removeItem(int Position) {
        chatMessages.remove(Position);
        chatAdapter.notifyDataSetChanged();
    }

    // Call video
    private void setVoiceCall(String targetUserID,String targetUserName){
        binding.imageCall.setIsVideoCall(false);
        binding.imageCall.setResourceID("zego_uikit_call"); // Please fill in the resource ID name that has been configured in the ZEGOCLOUD's console here.
        binding.imageCall.setInvitees(Collections.singletonList(new ZegoUIKitUser(targetUserID,targetUserName)));
    }

    private void setVideoCall(String targetUserID,String targetUserName){
        binding.imageVideoCall.setIsVideoCall(true);
        binding.imageVideoCall.setResourceID("zego_uikit_call"); // Please fill in the resource ID name that has been configured in the ZEGOCLOUD's console here.
        binding.imageVideoCall.setInvitees(Collections.singletonList(new ZegoUIKitUser(targetUserID,targetUserName)));
    }


    // Tương tác với CHAT GPT
    private void callApiInBackground(final String message) {
        // Tao new luong de thuc thi API
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                callAPI(message);
            }
        });
        thread.start();
    }
    // GPT
    private void  callAPI(String question){
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","gpt-3.5-turbo");
            JSONArray messageArr = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("role","user");
            obj.put("content",question);
            messageArr.put(obj);

            jsonBody.put("messages",messageArr);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("\n" +
                        "https://api.openai.com/v1/chat/completions")
                .header("Authorization","Bearer token")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull IOException e) {
                GPTResponse("So sorry! I am busy. Try again in a few minutes ");
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull okhttp3.Response response) throws IOException {
                if(response.isSuccessful()){
                    JSONObject  jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        GPTResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
//                    else if (response.code() == 429) {
//                        // Handle 429 error
//                        Log.d("test", "Error 429: Too Many Requests. Retrying...");
//                        // Implement retry logic here (delay and retry)
//                    }
                else {
                    GPTResponse("Failed to load response due to "+response.body().string());
                }
            }
        });

    }

    private void GPTRequest(String question){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,Constants.ChatGPT_ID.trim());
        message.put(Constants.KEY_MESSAGE,question.trim());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString().trim());
        }
        else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferecnceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferecnceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id );
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name );
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image );
            conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString().trim() );
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }
        binding.inputMessage.setText(null);
    }

    private void GPTResponse(String response){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,Constants.ChatGPT_ID.trim());//GPT
        message.put(Constants.KEY_RECEIVER_ID,preferecnceManager.getString(Constants.KEY_USER_ID)); //nguoi da request
        message.put(Constants.KEY_MESSAGE,response);//result from API
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion("Result is .....");
        }
        else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferecnceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferecnceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id );
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name );
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image );
            conversion.put(Constants.KEY_LAST_MESSAGE,"Result is .....");
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }

    }



    //Gửi ảnh
    private void sendImage(String url){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
        message.put(Constants.KEY_MESSAGE,url);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion("Image....");
        }
        else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferecnceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferecnceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id );
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name );
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image );
            conversion.put(Constants.KEY_LAST_MESSAGE,"Image....");
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID,preferecnceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferecnceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferecnceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,preferecnceManager.getString(Constants.KEY_NAME)+" đã gửi 1 ảnh");

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());
            }
            catch (Exception exception) {
                showToast(exception.getMessage());
            }
        }
    }

    //Chon anh tu thiet bi
    private void selectImage() {
        try {
            PackageManager pm = getPackageManager();
            int hasPerm = pm.checkPermission(Manifest.permission.CAMERA, getPackageName());
            if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                final CharSequence[] options = {"Take Photo", "Choose From Gallery","Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select Option");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Take Photo")) {
                            dialog.dismiss();
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, PICK_IMAGE_CAMERA);
                        } else if (options[item].equals("Choose From Gallery")) {
                            dialog.dismiss();
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, PICK_IMAGE_GALLERY);
                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            } else
                Toast.makeText(this, "Camera Permission error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Camera Permission error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }



    // Xử lý kết quả sau khi người dùng chọn hình ảnh
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        if (requestCode == PICK_IMAGE_CAMERA && resultCode == Activity.RESULT_OK) {
            try {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                // Chuyển đổi Bitmap thành Uri
                Uri imageUri = getImageUri(getApplicationContext(), imageBitmap);
                // Lưu hình ảnh vào Firebase Storage
                String imageName = UUID.randomUUID().toString();
                StorageReference imageRef = storageRef.child("images/" + imageName);
                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // Xử lý thành công
                             sendImage(imageName);
                        })
                        .addOnFailureListener(e -> {
                            // Xử lý khi lưu hình ảnh thất bại
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == PICK_IMAGE_GALLERY && resultCode == Activity.RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                // Lưu hình ảnh vào Firebase Storage
                String imageName = UUID.randomUUID().toString();
                StorageReference imageRef = storageRef.child("images/" + imageName);
                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // Xử lý thành công
                             sendImage(imageName);
                        })
                        .addOnFailureListener(e -> {
                            // Xử lý khi lưu hình ảnh thất bại
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //  chuyển đổi Bitmap thành Uri
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }




    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferecnceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }


    private void checkForConverstion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferecnceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id);
            checkForConversionRemotely(
                    receiverUser.id,
                    preferecnceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };


    // Tạo một cuộc trò chuyện mới khi chưa có
    private void addConversion( HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }


    // Update tin nhắn cuối cùng cho cuộc trò chuyện
    private void updateConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date()
        );
    }

    // Gửi tin nhắn
    private void sendMessage(String messs){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
        message.put(Constants.KEY_MESSAGE,messs);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null) {
            updateConversion(messs);
        }
        else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferecnceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferecnceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferecnceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id );
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name );
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image );
            conversion.put(Constants.KEY_LAST_MESSAGE,messs );
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID,preferecnceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferecnceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferecnceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,messs);

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());
            }
            catch (Exception exception) {
                showToast(exception.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }



    // Sự kiên xem có sự thay đổi gì ở danh sách tin nhắn không, tức là có tin nhắn nào mới không
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            chatMessages.sort((obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            }
            else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null ) {
            checkForConverstion();
        }
    };




    // Gửi thông báo
    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                }
                else {
                    showToast("Error: "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }




    // trang thái hoạt động
    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(receiverUser.id)
                .addSnapshotListener(ChatActivity.this, (value, error) -> {
                    if (error != null ) {
                        return;
                    }
                    if (value != null ){
                        if( value.getLong(Constants.KEY_AVAILABILITY) != null){
                            int availability = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY).intValue());
                            isReceiverAvailable = availability == 1;
                        }
                        receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                        if (receiverUser.image == null){
                            receiverUser.image = value.getString(Constants.KEY_IMAGE);
                            chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                            chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                        }
                    }
                    if (isReceiverAvailable){
                        binding.textAvailability.setVisibility(View.VISIBLE);
                    }
                    else{
                        binding.textAvailability.setVisibility(View.GONE);
                    }
                });
    }


    //Call
    private void startService(String senderID,String senderName) {
        Application application = getApplication(); // Android's application context
        long appID = 123456;   // yourAppID
        String appSign =".....";  // yourAppSign
//        String userName = userID; // yourUserID, userID should only contain numbers, English characters, and '_'.
        String userID = senderID; // yourUserID, userID should only contain numbers, English characters, and '_'.
        String userName = senderName;   // yourUserName

//        ZegoUIKitPrebuiltCallInvitationConfig callInvitationConfig = new ZegoUIKitPrebuiltCallInvitationConfig();
//        ZegoUIKitPrebuiltCallInvitationService.init(getApplication(), appID, appSign, userID, userName,callInvitationConfig);
        ZegoUIKitPrebuiltCallInvitationConfig callInvitationConfig = new ZegoUIKitPrebuiltCallInvitationConfig();
        callInvitationConfig.provider = new ZegoUIKitPrebuiltCallConfigProvider() {
            long durationSeconds = 0;
            @Override
            public ZegoUIKitPrebuiltCallConfig requireConfig(ZegoCallInvitationData invitationData) {
                ZegoUIKitPrebuiltCallConfig config = null;
                boolean isVideoCall = invitationData.type == ZegoInvitationType.VIDEO_CALL.getValue();
                long times = 0;
                boolean isGroupCall = invitationData.invitees.size() > 1;
                if (isVideoCall && isGroupCall) {
                    config = ZegoUIKitPrebuiltCallConfig.groupVideoCall();
                } else if (!isVideoCall && isGroupCall) {
                    config = ZegoUIKitPrebuiltCallConfig.groupVoiceCall();
                } else if (!isVideoCall) {
                    config = ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall();
                } else {
                    config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall();
                }
                config.durationConfig = new ZegoCallDurationConfig();
                config.durationConfig.isVisible = true;
                config.leaveCallListener = new ZegoUIKitPrebuiltCallFragment.LeaveCallListener() {
                    @Override
                    public void onLeaveCall() {
                        sendMessage("Đã thực hiện cuộc gọi trong "+String.valueOf(durationSeconds)+"s");
                        ZegoUIKitPrebuiltCallInvitationService.endCall();
                    }
                };
                config.durationConfig.durationUpdateListener = new DurationUpdateListener() {
                    @Override
                    public void onDurationUpdate(long seconds) {
                        durationSeconds = seconds;
                    }
                };
                return config;
            }
        };
        ZegoUIKitPrebuiltCallInvitationService.init(getApplication(), appID, appSign, userID, userName,callInvitationConfig);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoUIKitPrebuiltCallInvitationService.unInit();
    }

}