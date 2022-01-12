package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.whatshide.android.Notification.NotificationFunctions;
import com.whatshide.android.adapters.ChatAdapter;
import com.whatshide.android.adapters.ImageListAdapter;
import com.whatshide.android.listeners.ImageMessageListener;
import com.whatshide.android.listeners.MessageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;
import com.whatshide.android.utilities.UtilFun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
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

public class ChatActivity extends AppCompatActivity implements ImageMessageListener , MessageListener {
    CircleImageView profile;
    TextView name,status,openImageName,openImageDate;
    EditText message;
    ImageView select_image, send, back,openImageBack;
    PhotoView openImageView;
    FirebaseFirestore mDatabase;
    FirebaseAuth mAuth;
    FirebaseStorage firebaseStorage;
    String uid,mine;
    RecyclerView recyclerView;
    ChatAdapter adapter;
    List<Chat> chats;
    User me,you;

    RelativeLayout toProfile,openImageContainer;

    private boolean isOnline = false;
    private TextView message_status;

    private List<Chat> selectedChats = new ArrayList<>();

    private RelativeLayout selectedToolbar;
    private ImageView selectedClose;
    private ImageView selectedForward;
    private ImageView selectedCopy;

    private List<Uri> imageList = new ArrayList<>();
    private RelativeLayout selectedImage;
    private TextView selectedImageName;
    private RecyclerView selectedImageRecyclerView;
    private ImageView selectedImageBack;
    private ImageListAdapter imageListAdapter;
    private View cover;

    private static final int PICK_IMAGE_MULTIPLE = 1;

    private Chat conversation = null;
    boolean isImageSelected = false;
    boolean isOpenImageOpen = false;

    private String conversationId = null;

    //image zoom variables
    private ScaleGestureDetector scaleGestureDetector;
    private float FACTOR = 1.0f;
    private ImageView rotate;
    private boolean isMultiple = false;
    private boolean isSelectedToolbarOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initDB();

        bindView();

        getUserAndMe();

        getChats();

        setListener();

        if(conversationId != null){
            seenMessage();
        }

        Log.d("conversation id", "onCreate: "+conversationId);

    }



    private void seenMessage() {
        if(conversation.getReceiver().equals(mine)){
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put(Constants.KEY_STATUS,Constants.VALUE_SEEN);
            mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .update(hashMap);
        }
    }

    private void setListener() {
        back.setOnClickListener(backListener);
        send.setOnClickListener(sendListener);
        select_image.setOnClickListener(selectImageListener);
        selectedImageBack.setOnClickListener(selectedImageBackListener);
        toProfile.setOnClickListener(toProfileListener);
        openImageBack.setOnClickListener(openBackListener);
        rotate.setOnClickListener(rotateListener);
        cover.setOnClickListener(coverListener);
        selectedClose.setOnClickListener(selectedCloseListener);
        selectedForward.setOnClickListener(selectedForwardListener);
        selectedCopy.setOnClickListener(selectedCopyListener);
    }


    private View.OnClickListener selectedCopyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(selectedChats.size()>1){
                Toast.makeText(ChatActivity.this, "Select Only One Message!", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text",selectedChats.get(0).getMessage());
            manager.setPrimaryClip(clipData);
            closeSelectedToolbar();
            Toast.makeText(ChatActivity.this, "Message Copied Successfully!", Toast.LENGTH_SHORT).show();
        }
    };
    private View.OnClickListener selectedForwardListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openForwardActivity();
        }
    };

    private void openForwardActivity() {
        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
        intent.putExtra("forward_chats", (Serializable) selectedChats);
        startActivity(intent);
        finish();
    }

    private View.OnClickListener selectedCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSelectedToolbar();
        }
    };

    private View.OnClickListener coverListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSelectedToolbar();
        }
    };


    private View.OnClickListener rotateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            rotateImage();
        }
    };

    private void rotateImage() {
        openImageView.setRotationBy(90);
    }

    private View.OnClickListener openBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeImageViewContainerMethod();
        }
    };

    private View.OnClickListener toProfileListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(),UserProfileActivity.class);
            intent.putExtra(Constants.KEY_MOBILE,you.getMobile());
            startActivity(intent);
        }
    };

    private View.OnClickListener sendListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {
            if(isImageSelected){
                if(imageList.size() > 0){
                    isMultiple = true;
                    Log.d("conversation id", "onCreate: "+conversationId);
                    for(int i = 0; i < imageList.size() ; i++){
                        sendMessage(imageList.get(i), "Photo");
                    }
                    if(conversationId != null){
                        updateConversation("Photo");
                    }else{
                        HashMap<String, Object> conversation = new HashMap<>();
                        conversation.put(Constants.KEY_SENDER,mine);
                        conversation.put(Constants.KEY_RECEIVER,uid);
                        conversation.put(Constants.KEY_SENDER_NAME,me.getName());
                        conversation.put(Constants.KEY_SENDER_PROFILE_URL,me.getProfile_url());
                        conversation.put(Constants.KEY_RECEIVER_NAME,you.getName());
                        conversation.put(Constants.KEY_RECEIVER_PROFILE_URL,you.getProfile_url());
                        conversation.put(Constants.KEY_LAST_MESSAGE,"Photo");
                        conversation.put(Constants.KEY_TIMESTAMP,new Date());
                        conversation.put(Constants.KEY_STATUS,Constants.VALUE_DELIVERED);
                        addConversation(conversation);

                    }
                    closeSelectedImagePreview();
                }
            }else {
                isMultiple = false;
                String messageText = message.getText().toString();
                if(!messageText.equals("") && messageText != null){
                    sendMessage(null, messageText);
                }
            }
        }
    };

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener selectedImageBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSelectedImagePreview();
        }
    };

    private View.OnClickListener selectImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openFileChooser();
        }
    };

    private void getUserAndMe() {
        //checking current logged in user
        if(mAuth.getCurrentUser()==null){
            finish();
        }

        //getting intent content
        uid = getIntent().getStringExtra(Constants.KEY_UID);


        //getting user info
        mine = mAuth
                .getCurrentUser()
                .getPhoneNumber()
                .toString();

        UtilFun.setStatus(Constants.KEY_ONLINE,mine);

        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(mine)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult().exists()){
                            me = task.getResult().toObject(User.class);
                        }
                    }
                });
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error != null){
                            return;
                        }
                        if(value != null){
                            you = new User();
                            you.setStatus(value.getString(Constants.KEY_STATUS));
                            you.setName(value.getString(Constants.KEY_NAME));
                            you.setProfile_url(value.getString(Constants.KEY_PROFILE_URL));
                            you.setMobile(value.getString(Constants.KEY_MOBILE));
                            you.setLastSeen(value.getString(Constants.KEY_LAST_SEEN));
                            selectedImageName.setText("sending to "+ " "+you.getName());
                            name.setText(you.getName());
                            if(you.getStatus().equals(Constants.KEY_ONLINE)){
                                status.setText(you.getStatus());
                            }else{
                                status.setText("Last Seen At "+you.getLastSeen());
                            }
                            Glide.with(getApplicationContext()).load(you.getProfile_url()).into(profile);

                        }
                    }
                });
    }

    private void bindView() {
        name = (TextView) findViewById(R.id.name);
        profile = (CircleImageView) findViewById(R.id.profile);
        message = (EditText) findViewById(R.id.message);
        select_image = (ImageView) findViewById(R.id.image_select);
        send = (ImageView) findViewById(R.id.send);
        back = (ImageView) findViewById(R.id.back);
        status = (TextView) findViewById(R.id.status);
        toProfile = (RelativeLayout) findViewById(R.id.to_profile);

        openImageBack = (ImageView) findViewById(R.id.open_image_back);
        openImageContainer = (RelativeLayout) findViewById(R.id.open_image_container);
        openImageDate = (TextView) findViewById(R.id.open_image_date);
        openImageName = (TextView) findViewById(R.id.open_image_name);
        openImageView = (PhotoView) findViewById(R.id.open_image_view);

        message_status = (TextView) findViewById(R.id.message_status);

        rotate = (ImageView) findViewById(R.id.rotate);
        scaleGestureDetector = new ScaleGestureDetector(this,new ScaleListener());

        selectedImage = (RelativeLayout) findViewById(R.id.selected_image);
        selectedImageBack = (ImageView) findViewById(R.id.selected_image_back);
        selectedImageName = (TextView) findViewById(R.id.selected_image_name);
        selectedImageRecyclerView = (RecyclerView) findViewById(R.id.selected_image_recycler_view);
        imageListAdapter = new ImageListAdapter(imageList,getApplicationContext());
        selectedImageRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.VERTICAL,
                false));
        selectedImageRecyclerView.setAdapter(imageListAdapter);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        selectedClose = (ImageView) findViewById(R.id.selected_close);
        selectedToolbar = (RelativeLayout) findViewById(R.id.selected_toolbar);
        selectedForward = (ImageView) findViewById(R.id.selected_forward);
        selectedCopy = (ImageView) findViewById(R.id.selected_copy);

        cover = (View) findViewById(R.id.cover);


        chats = new ArrayList<>();
        adapter = new ChatAdapter(chats,getApplicationContext(),this,this);
        recyclerView.setAdapter(adapter);
    }

    private void initDB() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void getChats() {

        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,mine)
                .whereEqualTo(Constants.KEY_RECEIVER,uid)
                .addSnapshotListener(eventListener);
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,uid)
                .whereEqualTo(Constants.KEY_RECEIVER,mine)
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
                        chat.setId(documentChange.getDocument().getId());
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
                                chats.get(i).setImage_url(documentChange.getDocument().getString(Constants.KEY_IMAGE_URL));
                                chats.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                            }
                        }
                    }
                    else if(documentChange.getType() == DocumentChange.Type.REMOVED){
                        chats.remove(documentChange.getOldIndex());
                        adapter.notifyItemRemoved(documentChange.getOldIndex());
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


                if(conversationId == null){
                    checkForConversation();
                }else {
                    if(isOnline){
                        seenMessage();
                    }
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendMessage(Uri image_url,String messageText) {
        //get current time

        Log.d("conversation id", "onCreate: "+conversationId);

        HashMap<String, Object> chat = new HashMap<>();
        chat.put(Constants.KEY_MESSAGE,messageText);
        chat.put(Constants.KEY_SENDER,mine);
        chat.put(Constants.KEY_RECEIVER,uid);
        chat.put(Constants.KEY_TIMESTAMP,new Date());
        chat.put(Constants.KEY_STATUS,Constants.VALUE_DELIVERED);


        if(image_url != null){
            byte[] data =  compressImage(image_url);
            String encodedImage = Base64.getEncoder().encodeToString(data);
            chat.put(Constants.KEY_IMAGE_URL,encodedImage);
        }
        //add new chat document in the collection messages
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .add(chat);

        //set conversation

        if(!isMultiple){
            if(conversationId != null){
                updateConversation(messageText);
            }else{
                HashMap<String, Object> conversation = new HashMap<>();
                conversation.put(Constants.KEY_SENDER,mine);
                conversation.put(Constants.KEY_RECEIVER,uid);
                conversation.put(Constants.KEY_SENDER_NAME,me.getName());
                conversation.put(Constants.KEY_SENDER_PROFILE_URL,me.getProfile_url());
                conversation.put(Constants.KEY_RECEIVER_NAME,you.getName());
                conversation.put(Constants.KEY_RECEIVER_PROFILE_URL,you.getProfile_url());
                conversation.put(Constants.KEY_LAST_MESSAGE,messageText);
                conversation.put(Constants.KEY_TIMESTAMP,new Date());
                conversation.put(Constants.KEY_STATUS,Constants.VALUE_DELIVERED);
                addConversation(conversation);

            }
        }
        //empty the editText
        message.setText(null);

        //send Notification removed because pranjal said so//
     /*   if(you.getStatus().equals(Constants.KEY_OFFLINE)){
          //  NotificationFunctions.sendNotification(getApplicationContext(),
                    uid,
                    me.getName(),
                    messageText,
                    mine
            );
        }*/


    }

    public String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation){
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        conversationId = task.getResult().getId();
                    }
                });
    }

    private void updateConversation(String message){
        DocumentReference documentReference = mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date(),
                Constants.KEY_STATUS,Constants.VALUE_DELIVERED
        );
    }

    private void checkForConversation(){
        Log.d("is", "checkForConversation: "+ "called");
        if(chats.size() != 0){
            checkForConversationRemotely(mine,uid);
        }
        checkForConversationRemotely(uid,mine);
    }

    private void checkForConversationRemotely(String sender, String receiver){
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER,sender)
                .whereEqualTo(Constants.KEY_RECEIVER,receiver)
                .addSnapshotListener(conversationEventListener);
    }

    private final EventListener<QuerySnapshot> conversationEventListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
            if(error != null){
                return;
            }
            if(value != null && value.getDocuments().size()>0){
                DocumentChange documentChange = value.getDocumentChanges().get(0);
                if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                    conversationId = documentChange.getDocument().getId();
                    conversation = new Chat();
                    conversation.setSender(documentChange.getDocument().getString(Constants.KEY_SENDER));
                    conversation.setReceiver(documentChange.getDocument().getString(Constants.KEY_RECEIVER));
                    conversation.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                    conversation.setStatus(documentChange.getDocument().getString(Constants.KEY_STATUS));
                    if(conversation.getSender().equals(mine) && conversation.getStatus().equals(Constants.VALUE_SEEN)){
                        message_status.setVisibility(View.VISIBLE);
                        message_status.setText(conversation.getStatus());
                    }else{
                        message_status.setVisibility(View.INVISIBLE);
                    }
                }else{
                    conversationId = documentChange.getDocument().getId();
                    conversation = new Chat();
                    conversation.setSender(documentChange.getDocument().getString(Constants.KEY_SENDER));
                    conversation.setReceiver(documentChange.getDocument().getString(Constants.KEY_RECEIVER));
                    conversation.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                    conversation.setStatus(documentChange.getDocument().getString(Constants.KEY_STATUS));
                    if(conversation.getSender().equals(mine) && conversation.getStatus().equals(Constants.VALUE_SEEN)){
                        message_status.setVisibility(View.VISIBLE);
                        message_status.setText(conversation.getStatus());
                    }else{
                        message_status.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    };

    private void openFileChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i,Constants.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK && null != data) {
            // Get the Image from data
            if (data.getClipData() != null) {
                ClipData mClipData = data.getClipData();
                for (int i = 0; i < mClipData.getItemCount(); i++) {
                    ClipData.Item item = mClipData.getItemAt(i);
                    Uri uri = item.getUri();
                    imageList.add(uri);
                }
            }
            else if(data.getData()!=null){
                imageList.add(data.getData());

            }

            openSelectedImagePreview();
        } else {
            Toast.makeText(this, "You haven't picked Image",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openSelectedImagePreview() {
        imageListAdapter = new ImageListAdapter(imageList,getApplicationContext());
        selectedImageRecyclerView.setAdapter(imageListAdapter);
        isImageSelected = true;
        selectedImage.setVisibility(View.VISIBLE);
        selectedImage.setClickable(true);
    }

    private void closeSelectedImagePreview() {
        isImageSelected = false;
        imageList = new ArrayList<>();
        imageListAdapter.notifyDataSetChanged();
        selectedImage.setVisibility(View.INVISIBLE);
        selectedImage.setClickable(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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

    private void closeImageViewContainerMethod(){
        openImageContainer.setVisibility(View.GONE);
        isOpenImageOpen = false;
    }

    private void openImageViewContainerMethod(){
        openImageContainer.setVisibility(View.VISIBLE);
        openImageContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        isOpenImageOpen = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("conversation id", "onCreate: "+conversationId);

        isOnline = true;
        if(conversationId != null){
            seenMessage();
        }
        UtilFun.setStatus(Constants.KEY_ONLINE,mine);

    }

    @Override
    protected void onRestart() {
        if(conversationId != null){
            seenMessage();
        }
        super.onRestart();
        isOnline = true;
        UtilFun.setStatus(Constants.KEY_ONLINE,mine);

    }

    @Override
    protected void onResume() {
        super.onResume();
        isOnline = true;
        if(conversationId != null){
            seenMessage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnline = false;
    }

    @Override
    public void onBackPressed() {
        if (isImageSelected){
            closeSelectedImagePreview();
        }else if(isOpenImageOpen){
            closeImageViewContainerMethod();
        }
        else {
            super.onBackPressed();
            UtilFun.setLastSeen(mine);
            isOnline = true;
            UtilFun.setStatus(Constants.KEY_OFFLINE,mine);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        UtilFun.setLastSeen(mine);
        isOnline = false;
        UtilFun.setStatus(Constants.KEY_OFFLINE,mine);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UtilFun.setLastSeen(mine);
        UtilFun.setStatus(Constants.KEY_OFFLINE,mine);
        isOnline = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onImageMessageClicked(Chat chat) {
        openImageView.setImageBitmap(UtilFun.getBitmapFromEncodeImage(chat.getImage_url()));
        if(chat.getSender().equals(mine)){
            openImageName.setText("You");
        }else{
            openImageName.setText(you.getName());
        }
        openImageDate.setText(chat.getTime());
        openImageViewContainerMethod();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return scaleGestureDetector.onTouchEvent(event);

    }

    @Override
    public void onMessageSelect(Chat chat) {
        openSelectedToolbar();
        selectedChats.add(chat);
    }

    @Override
    public void onMessageRemoved(Chat chat) {
        selectedChats.remove(chat);
    }

    private void openSelectedToolbar() {
        isSelectedToolbarOpen = true;
        selectedToolbar.setVisibility(View.VISIBLE);
        selectedToolbar.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }

    private void closeSelectedToolbar() {
        isSelectedToolbarOpen = false;
        selectedToolbar.setVisibility(View.GONE);
        selectedChats = new ArrayList<>();
        adapter.setSelected(false);
    }

    class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d("working", "onScale: ");
            FACTOR *= detector.getScaleFactor();
            FACTOR = Math.max(0.1f,Math.min(FACTOR,10.f));
            openImageView.setScaleX(FACTOR);
            openImageView.setScaleY(FACTOR);
            return true;
        }
    }
}