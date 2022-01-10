package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whatshide.android.adapters.AdminChatAdapter;
import com.whatshide.android.listeners.ImageMessageListener;
import com.whatshide.android.listeners.MessageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;
import com.whatshide.android.utilities.UtilFun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminChatActivity extends AppCompatActivity implements MessageListener, ImageMessageListener {
    private static final int PICK_IMAGE_REQUEST = 1;
    private FirebaseFirestore mDatabase;
    private FirebaseAuth mAuth;
    private String sender, receiver, senderName, receiverName;
    private RecyclerView recyclerView;
    private TextView senderTextView, receiverTextView;
    private CircleImageView senderProfile,receiverProfile;
    private User userSender,userReceiver;
    private List<Chat> chats;
    private AdminChatAdapter adapter;

    private RelativeLayout selectedToolbar;
    private ImageView selectedDelete, selectedEdit, selectedCopy, selectedClose;
    private Chat selectedChat;

    private ImageView deleteConversation;
    private LinearLayout updateMessageContainer;
    private EditText updateMessageEditText;
    private Button updateMessageSubmit;
    private View cover;
    private ImageView openImageBack,back;
    private RelativeLayout openImageContainer;
    private TextView openImageDate, openImageName;
    private PhotoView openImageView;
    private boolean isOpenImageOpen;

    private LinearLayout updateImageContainer;
    private PhotoView updateImageView;
    private Button updateImageSubmit;

    private Uri updateImageUrl;
    private boolean isSelectedToolbarOpen;

    private String conversationId;
    private ImageView rotate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        initDB();

        getIntentData();

        getUserData();

        bindView();


        getChats();

        setListeners();


    }

    private void setListeners() {
        selectedClose.setOnClickListener(closeListener);
        selectedEdit.setOnClickListener(editListener);
        selectedDelete.setOnClickListener(deleteListener);
        selectedCopy.setOnClickListener(copyListener);

        back.setOnClickListener(backListener);
        openImageBack.setOnClickListener(openImageBackListener);

        cover.setOnClickListener(coverListener);
        updateImageSubmit.setOnClickListener(updateImageSubmitListener);
        updateImageView.setOnClickListener(updateImageViewListener);
        updateMessageSubmit.setOnClickListener(updateMessageSubmitListener);
        deleteConversation.setOnClickListener(deleteConversationListener);
        rotate.setOnClickListener(rotateListener);
    }

    private View.OnClickListener rotateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            rotateImage();
        }
    };

    private void rotateImage() {
        openImageView.setRotationBy(90);
    }
    private View.OnClickListener updateImageSubmitListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {

            if(updateImageUrl == null){
                return;
            }
            byte[] data =  compressImage(updateImageUrl);
            String encodedImage = Base64.getEncoder().encodeToString(data);
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put(Constants.KEY_IMAGE_URL,encodedImage);
            mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(selectedChat.getId())
                    .update(hashMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                closeUpdateImageContainer();
                                closeSelectedToolbar();
                                Toast.makeText(AdminChatActivity.this, "updated successfully!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    };

    private void closeUpdateImageContainer() {
        updateImageContainer.setVisibility(View.GONE);
        cover.setVisibility(View.GONE);
    }

    private View.OnClickListener coverListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeUpdateImageContainer();
            closeUpdateMessageContainer();
        }
    };

    private View.OnClickListener deleteConversationListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            askForDeleteConversation();
        }
    };

    private void askForDeleteConversation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Conversation");
        builder.setMessage("Do you really want this?");
        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteConversationMethod();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void deleteConversationMethod() {
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,sender)
                .whereEqualTo(Constants.KEY_RECEIVER,receiver)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null && task.getResult().size()>0){
                            for(QueryDocumentSnapshot querySnapshot : task.getResult()){
                                querySnapshot.getReference().delete();
                            }
                        }
                    }
                });
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,receiver)
                .whereEqualTo(Constants.KEY_RECEIVER,sender)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null && task.getResult().size()>0){
                            for(QueryDocumentSnapshot querySnapshot : task.getResult()){
                                querySnapshot.getReference().delete();
                            }
                        }
                    }
                });
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER,sender)
                .whereEqualTo(Constants.KEY_RECEIVER,receiver)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                            task.getResult().getDocuments().get(0).getReference().delete();
                        }
                    }
                });
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER,receiver)
                .whereEqualTo(Constants.KEY_RECEIVER,sender)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                            task.getResult().getDocuments().get(0).getReference().delete();
                        }
                    }
                });

    }


    private View.OnClickListener updateImageViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openFileChooser();
        }
    };

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();

        }
    };

    private View.OnClickListener deleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteSelected();
        }
    };

    private View.OnClickListener openImageBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeImageViewContainerMethod();
        }
    };

    private View.OnClickListener updateMessageSubmitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String message = updateMessageEditText.getText().toString();
            if( message != null && !message.equals("")){
                Map<String, Object> hashMap = new HashMap<>();
                hashMap.put(Constants.KEY_MESSAGE, message);
                mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                        .document(selectedChat.getId())
                        .update(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            if(chats.get(chats.size()-1).getId().equals(selectedChat.getId())){
                                HashMap<String, Object> hashMapC = new HashMap<>();
                                hashMapC.put(Constants.KEY_LAST_MESSAGE,message);
                                mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                        .document(conversationId)
                                        .update(hashMapC);
                            }
                            adapter.notifyDataSetChanged();
                            Toast.makeText(AdminChatActivity.this, "Message Updated Successfully!", Toast.LENGTH_SHORT).show();
                            closeSelectedToolbar();
                            closeUpdateMessageContainer();
                        }
                    }
                });
            }
        }
    };

    private void closeUpdateMessageContainer() {


        updateMessageContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_close));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_out));
        updateMessageEditText.setText(null);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMessageContainer.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
            }
        },200);

    }

    private void deleteSelected() {
        if (selectedChat != null){
            mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(selectedChat.getId())
                    .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        if(chats.get(chats.size()-1).getId().equals(selectedChat.getId())){
                            HashMap<String, Object> hashMapC = new HashMap<>();
                            hashMapC.put(Constants.KEY_LAST_MESSAGE,"");
                            mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                    .document(conversationId)
                                    .update(hashMapC);
                        }
                        chats.remove(selectedChat);
                        adapter.notifyDataSetChanged();
                        closeSelectedToolbar();

                        if(chats.size()<1){
                            mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                    .document(conversationId)
                                    .delete();
                        }

                    }
                }
            });
        }
    }


    private View.OnClickListener editListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (selectedChat != null){
                if(selectedChat.getImage_url() != null){
                    openUpdateImageContainer();
                }else{
                    openUpdateMessageContainer();
                }
            }
        }
    };

    private void openUpdateImageContainer() {
        updateImageContainer.setVisibility(View.VISIBLE);
        updateImageContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        cover.setVisibility(View.VISIBLE);
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }

    private void openUpdateMessageContainer() {
        updateMessageContainer.setVisibility(View.VISIBLE);
        cover.setVisibility(View.VISIBLE);
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
        updateMessageContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
    }

    private View.OnClickListener closeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSelectedToolbar();
        }
    };

    private View.OnClickListener copyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(selectedChat.getImage_url() != null){
                return;
            }
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text",selectedChat.getMessage());
            manager.setPrimaryClip(clipData);
            Toast.makeText(AdminChatActivity.this, "Message Copied Successfully!", Toast.LENGTH_SHORT).show();
        }
    };



    private void bindView() {
        chats = new ArrayList<>();

        senderTextView = (TextView) findViewById(R.id.sender_name_view);
        receiverTextView = (TextView) findViewById(R.id.receiver_name_view);
        senderProfile = (CircleImageView) findViewById(R.id.sender_profile);
        receiverProfile = (CircleImageView) findViewById(R.id.receiver_profile);
        rotate = (ImageView) findViewById(R.id.rotate);

        selectedClose = (ImageView) findViewById(R.id.selected_close);
        selectedCopy = (ImageView) findViewById(R.id.selected_copy);
        selectedDelete = (ImageView) findViewById(R.id.selected_delete);
        selectedEdit = (ImageView) findViewById(R.id.selected_edit);
        selectedToolbar = (RelativeLayout) findViewById(R.id.selected_toolbar);

        back = (ImageView) findViewById(R.id.back);

        updateImageContainer = (LinearLayout) findViewById(R.id.update_image_container);
        updateImageSubmit = (Button) findViewById(R.id.update_image_submit);
        updateImageView = (PhotoView) findViewById(R.id.update_image_view);

        updateMessageContainer = (LinearLayout) findViewById(R.id.update_message_container);
        updateMessageSubmit = (Button) findViewById(R.id.update_message_submit);
        updateMessageEditText = (EditText) findViewById(R.id.update_message_edit_text);
        cover = (View) findViewById(R.id.cover);

        openImageBack = (ImageView) findViewById(R.id.open_image_back);
        openImageContainer = (RelativeLayout) findViewById(R.id.open_image_container);
        openImageDate = (TextView) findViewById(R.id.open_image_date);
        openImageName = (TextView) findViewById(R.id.open_image_name);
        openImageView = (PhotoView) findViewById(R.id.open_image_view);

        deleteConversation = (ImageView) findViewById(R.id.delete_conversation);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new AdminChatAdapter(getApplicationContext(),chats,sender,senderName,receiver,receiverName,this,this);
        recyclerView.setAdapter(adapter);
    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getIntentData() {
        sender = getIntent().getStringExtra(Constants.KEY_SENDER);
        receiver = getIntent().getStringExtra(Constants.KEY_RECEIVER);
        senderName = getIntent().getStringExtra(Constants.KEY_SENDER_NAME);
        receiverName = getIntent().getStringExtra(Constants.KEY_RECEIVER_NAME);
        conversationId = getIntent().getStringExtra(Constants.KEY_ID);
    }

    private void getUserData() {
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(sender)
                .get()
                .addOnCompleteListener(senderListener);
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(receiver)
                .get()
                .addOnCompleteListener(receiverListener);

    }


    private OnCompleteListener<DocumentSnapshot> senderListener = new OnCompleteListener<DocumentSnapshot>() {
        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            if(task.isSuccessful() && task.getResult() != null){
                userSender = task.getResult().toObject(User.class);
                Glide.with(getApplicationContext())
                        .load(userSender.getProfile_url())
                        .into(senderProfile);
                senderTextView.setText(userSender.getName() + " and ");
            }
        }
    };
    private OnCompleteListener<DocumentSnapshot> receiverListener = new OnCompleteListener<DocumentSnapshot>() {
        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            if(task.isSuccessful() && task.getResult() != null){
                userReceiver = task.getResult().toObject(User.class);
                Glide.with(getApplicationContext())
                        .load(userReceiver.getProfile_url())
                        .into(receiverProfile);
                receiverTextView.setText(userReceiver.getName());

            }
        }
    };


    private void getChats() {

        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,sender)
                .whereEqualTo(Constants.KEY_RECEIVER,receiver)
                .addSnapshotListener(eventListener);
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,receiver)
                .whereEqualTo(Constants.KEY_RECEIVER,sender)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
            if(error!=null){
                return;
            }
            if(value!=null && value.getDocumentChanges().size()>0){
                int count = chats.size();
                boolean isModified = false;
                for(DocumentChange documentChange:value.getDocumentChanges()){
                    if(documentChange.getType() == DocumentChange.Type.ADDED){
                        Chat chat = new Chat();
                        chat.setId(documentChange.getDocument().getId().toString());
                        chat.setStatus(documentChange.getDocument().getString(Constants.KEY_STATUS));
                        chat.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                        chat.setSender(documentChange.getDocument().getString(Constants.KEY_SENDER));
                        chat.setReceiver(documentChange.getDocument().getString(Constants.KEY_RECEIVER));
                        chat.setTime(getReadableDateTime(
                                documentChange.getDocument()
                                        .getDate(Constants.KEY_TIMESTAMP)
                                )
                        );

                        if(documentChange.getDocument().getString(Constants.KEY_IMAGE_URL) != null){
                            chat.setImage_url(documentChange.getDocument().getString(Constants.KEY_IMAGE_URL));
                        }
                        chat.dateObj = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        chats.add(chat);
                    }
                    else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                        isModified = true;
                        for(int i = 0; i < chats.size() ; i++){
                            String sender = documentChange.getDocument().getString(Constants.KEY_SENDER);
                            String receiver = documentChange.getDocument().getString(Constants.KEY_RECEIVER);
                            if(chats.get(i).getId().equals(documentChange.getDocument().getId())){
                                chats.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                                chats.get(i).setImage_url(documentChange.getDocument().getString(Constants.KEY_IMAGE_URL));
                            }
                        }
                    }
                }
                Collections.sort(chats,(obj1, obj2) -> obj1.dateObj.compareTo(obj2.dateObj));
                if(count == 0){
                    adapter.notifyDataSetChanged();
                }else{
                    adapter.notifyDataSetChanged();
                    adapter.notifyItemRangeInserted(chats.size(), chats.size());
                }
                if(!isModified){
                    recyclerView.scrollToPosition(chats.size()-1);
                }
            }
        }
    };


    public String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    @Override
    public void onMessageSelect(Chat chat) {
        openSelectedToolbar();
        selectedChat = chat;

    }

    private void openSelectedToolbar() {
        isSelectedToolbarOpen = true;
        selectedToolbar.setVisibility(View.VISIBLE);
        selectedToolbar.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }


    private void closeSelectedToolbar() {
        isSelectedToolbarOpen = false;
        selectedToolbar.setVisibility(View.GONE);
        selectedChat = null;
        adapter.setSelected(false);
        closeUpdateMessageContainer();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onImageMessageClicked(Chat chat) {
        openImageView.setImageBitmap(UtilFun.getBitmapFromEncodeImage(chat.getImage_url()));
        openImageDate.setText(chat.getTime());
        if (chat.getSender().equals(sender)) {
            openImageName.setText(senderName);
        }else{
            openImageName.setText(receiverName);
        }
        openImageViewContainerMethod();
    }


    private void closeImageViewContainerMethod(){
        openImageContainer.setVisibility(View.GONE);
        isOpenImageOpen = false;
    }


    private void openFileChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i,Constants.PICK_IMAGE_REQUEST);
    }
    private void openImageViewContainerMethod(){
        openImageContainer.setVisibility(View.VISIBLE);
        openImageContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        isOpenImageOpen = true;
    }

    private byte[] compressImage(Uri image_url) {
        Bitmap bmp = null;
        try {
            bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), image_url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, UtilFun.getQualityFactor(bmp.getByteCount()), baos);
        return baos.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            updateImageUrl = data.getData();

            Glide.with(getApplicationContext()).load(updateImageUrl).into(updateImageView);
        }
    }


    @Override
    public void onBackPressed() {
        if(isOpenImageOpen){
            closeImageViewContainerMethod();
        }else if(isSelectedToolbarOpen){
            closeSelectedToolbar();
        }
        else{
            super.onBackPressed();
        }
    }
}