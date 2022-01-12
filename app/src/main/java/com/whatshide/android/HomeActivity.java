package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.whatshide.android.Notification.Token;
import com.whatshide.android.adapters.RecentAdapter;
import com.whatshide.android.listeners.ProfileImageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.utilities.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements ProfileImageListener {
    FirebaseAuth mAuth;
    FirebaseFirestore mDatabase;
    RecyclerView recyclerView;
    String mine;
    ProgressBar progressBar;
    TextView notice;
    CircleImageView FAB;
    private MaterialSearchView searchView;
    List<Chat> recentConversations;
    RecentAdapter adapter;
    RelativeLayout tabs;

    private ImageView profileImageView;
    private TextView profileTextView;
    private RelativeLayout profileContainer;
    private View cover;
    private Chat forwardChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //bind all views
        bindView();

        getIntentData();
        //initiate database and auth
        initDB();

        //check for user
        checkCurrentUser();

        getAllRecent();

        setToken();

        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),SearchActivity.class));
            }
        });

        cover.setOnClickListener(coverListener);

    }

    private void getIntentData() {
        Gson gson = new Gson();
        forwardChat = gson.fromJson(getIntent().getStringExtra("chat"), Chat.class);
        if(forwardChat == null){
            return;
        }

        openForwardToolbar();
    }

    private void openForwardToolbar() {

    }

    private void closeForwardToolbar(){

    }
    private View.OnClickListener coverListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeProfile();
        }
    };


    private void setToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Token token = new Token(s);
                mDatabase.collection(Constants.KEY_COLLECTION_TOKENS).document(mine).set(token);
            }
        });
    }


    private void checkCurrentUser() {
        if(mAuth.getCurrentUser().equals(null)){
            finish();
        }
        mine = mAuth.getCurrentUser().getPhoneNumber().toString();
    }

    private void initDB() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
    }

    private MaterialSearchView.OnQueryTextListener queryTextListener = new MaterialSearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if(adapter.getItemCount()==0){
                notice.setVisibility(View.VISIBLE);
                notice.setText("No results found for '"+query+"'");
            }else{
                notice.setVisibility(View.INVISIBLE);
                notice.setText("");
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.length() == 0){
                notice.setVisibility(View.INVISIBLE);
            }
            adapter.getFilter().filter(newText);
            if(adapter.getItemCount()==0){
                notice.setVisibility(View.VISIBLE);
                notice.setText("No results found for '"+newText+"'");
            }else{
                notice.setText("");
                notice.setVisibility(View.INVISIBLE);
            }
            return true;
        }
    };

    private void bindView() {
        recentConversations = new ArrayList<>();
        adapter = new RecentAdapter(recentConversations, getApplicationContext(), this);
        androidx.appcompat.widget.Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Whatshide");
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.VERTICAL,false));
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        notice = (TextView) findViewById(R.id.notice);
        FAB = (CircleImageView) findViewById(R.id.newChat);
        recyclerView.setAdapter(adapter);
        tabs = (RelativeLayout) findViewById(R.id.tablayout);
        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setCursorDrawable(R.drawable.color_cursor_white);

        profileImageView = (ImageView) findViewById(R.id.profile_image_view);
        profileContainer = (RelativeLayout) findViewById(R.id.profile_container);
        profileTextView = (TextView) findViewById(R.id.profile_name);
        cover = (View) findViewById(R.id.cover);

        searchView.setOnQueryTextListener(queryTextListener);

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                tabs.setVisibility(View.GONE);
                tabs.setMinimumHeight(0);
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                notice.setVisibility(View.INVISIBLE);
                tabs.setVisibility(View.VISIBLE);

                //Do some magic
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.setting:
                startActivity(new Intent(getApplicationContext(),ProfileActivity.class));
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.search_icon);
        searchView.setMenuItem(item);
        return true;
    }



    private void getAllRecent() {
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER,mine)
                .addSnapshotListener(eventListener);
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER,mine)
                .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot> eventListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
            if(error != null){
                return;
            }
            if(value != null){
                for(DocumentChange documentChange : value.getDocumentChanges()){
                    if(documentChange.getType() == DocumentChange.Type.ADDED){
                        String sender = documentChange.getDocument().getString(Constants.KEY_SENDER);
                        String receiver = documentChange.getDocument().getString(Constants.KEY_RECEIVER);

                        Chat chat = new Chat();
                        chat.setId(documentChange.getDocument().getId());
                        chat.setSender(sender);
                        chat.setReceiver(receiver);
                        if(mine.equals(sender)){
                            chat.conversationImageUrl = documentChange.getDocument().getString(Constants.KEY_RECEIVER_PROFILE_URL);
                            chat.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                            chat.conversationId = documentChange.getDocument().getString(Constants.KEY_RECEIVER);

                        }else {
                            chat.conversationImageUrl = documentChange.getDocument().getString(Constants.KEY_SENDER_PROFILE_URL);
                            chat.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                            chat.conversationId = documentChange.getDocument().getString(Constants.KEY_SENDER);

                        }

                        chat.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                        chat.dateObj = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        recentConversations.add(chat);
                    }
                    else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                        for(int i=0; i <recentConversations.size(); i++){
                            String sender = documentChange.getDocument().getString(Constants.KEY_SENDER);
                            String receiver = documentChange.getDocument().getString(Constants.KEY_RECEIVER);
                            if(recentConversations.get(i).getSender().equals(sender) && recentConversations.get(i).getReceiver().equals(receiver)){
                                recentConversations.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                                recentConversations.get(i).dateObj = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                                break;
                            }
                        }
                    }
                    else if(documentChange.getType() == DocumentChange.Type.REMOVED){
                        Log.d("index", "onEvent: "+documentChange.getOldIndex() +"-"+documentChange.getDocument().getId());
                        for(int i=0;i<recentConversations.size();i++){
                            if(recentConversations.get(i).getId().equals(documentChange.getDocument().getId())){
                                recentConversations.remove(i);
                                adapter.notifyItemRemoved(i);
                            }
                        }
                    }

                }
                Collections.sort(recentConversations,(obj1,obj2) -> obj2.dateObj.compareTo(obj1.dateObj));
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
                progressBar.setVisibility(View.INVISIBLE);
                if(recentConversations.size()>0){
                    notice.setVisibility(View.GONE);
                }else{
                    notice.setVisibility(View.VISIBLE);
                }
            }else {
                if(recentConversations.size()>0){
                    notice.setVisibility(View.GONE);
                }else{
                    notice.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private void openProfile(){
        profileContainer.setVisibility(View.VISIBLE);
        cover.setVisibility(View.VISIBLE);
        profileContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }

    private void closeProfile(){
        profileContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_close));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                profileContainer.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
            }
        },200);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onProfileClicked(Chat chat) {
        Glide.with(getApplicationContext())
                .load(chat.conversationImageUrl)
                .into(profileImageView);
        profileTextView.setText(chat.conversationName);
        openProfile();
    }
}