package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.whatshide.android.adapters.SearchAdapter;
import com.whatshide.android.listeners.UserListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements UserListener {
    ArrayList<User> users;
    RecyclerView recyclerView;
    SearchAdapter adapter;
    ProgressBar progressBar;
    FirebaseFirestore mDatabase;
    FirebaseAuth mAuth;
    TextView notice;
    String mine;

    private List<Chat> forwardChats;
    private String MODE = Constants.MODE_SEARCH;
    List<String> allContacts,SPContacts,firestoreContacts;
    private MaterialSearchView searchView;
    androidx.appcompat.widget.Toolbar myToolbar;
    private User me;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initDB();

        getIntentData();

        bindView();

        getUserInfo();

        loadData1();

    }

    private void getIntentData() {
        forwardChats = (List<Chat>) getIntent().getSerializableExtra("forward_chats");
        if(forwardChats == null){
            MODE = Constants.MODE_SEARCH;
        }else{
            MODE = Constants.MODE_FORWARD;
        }
    }

    private void loadData1() {
        //load contacts from Shared preference
        SPContacts = getContactsFromSP();

        //check for the existence
        if(SPContacts.size() == 0) {
            allContacts = getContacts();
            mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful() && task.getResult() != null){
                                for(DocumentSnapshot documentSnapshot : task.getResult()){
                                    User user = documentSnapshot.toObject(User.class);
                                    firestoreContacts.add(user.getMobile().substring(3));
                                }
                                firestoreContacts.retainAll(allContacts);
                                SPContacts = firestoreContacts;
                                saveContacts(SPContacts);
                                showSyncedUsers();
                            }
                        }
                    });
        }else {
            showSyncedUsers();
        }
    }

    private void showSyncedUsers() {

        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null){
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                User user = documentSnapshot.toObject(User.class);
                                if(isInSPContacts(user) && !user.getMobile().equals(mine)){
                                    users.add(user);
                                }
                            }
                            adapter = new SearchAdapter(users,getApplicationContext(),SearchActivity.this);
                            recyclerView.setAdapter(adapter);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private boolean isInSPContacts(User user) {
        for (int i = 0; i < SPContacts.size() ; i++){
            if(SPContacts.get(i).equals(user.getMobile().substring(3))){
                return true;
            }
        }
        return false;
    }


    private void getUserInfo() {
        if(mAuth.getCurrentUser()!=null){
            mine = mAuth.getCurrentUser().getPhoneNumber().toString();

            mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                    .document(mine)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful() && task.getResult().exists()){
                            me = task.getResult().toObject(User.class);
                        }
                    });
            
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        searchView.setMenuItem(item);
        return true;
    }

    public TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.getFilter().filter(s);
            if(adapter.getItemCount()==0){
                notice.setText("No results found for '"+s+"'");
            }else{
                notice.setText("");
            }

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(adapter.getItemCount()==0){
                notice.setText("No results found for '"+s+"'");
            }else{
                notice.setText("");
            }
        }
    };


    private void bindView() {
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if(MODE.equals(Constants.MODE_FORWARD)){
            getSupportActionBar().setTitle("Forward To");
        }
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        notice = (TextView) findViewById(R.id.notice);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.VERTICAL,false));

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setVoiceSearch(true);
        searchView.setCursorDrawable(R.drawable.color_cursor_white);

        users = new ArrayList<>();
        firestoreContacts = new ArrayList<>();
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(adapter.getItemCount()==0){
                    notice.setText("No results found for '"+query+"'");
                }else{
                    notice.setText("");
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                if(adapter.getItemCount()==0){
                    notice.setText("No results found for '"+newText+"'");
                }else{
                    notice.setText("");
                }
                return true;
            }
        });
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                //Do some magic
            }
        });
        myToolbar.setNavigationOnClickListener(v -> SearchActivity.super.onBackPressed());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.homeAsUp:
                super.onBackPressed();
                break;
            case R.id.refresh:
                deleteContactsListAndRefresh();
                break;

        }
        return true;
    }

    private void deleteContactsListAndRefresh() {
        saveContacts(null);
        SPContacts = new ArrayList<>();
        allContacts = new ArrayList<>();
        users = new ArrayList<>();
        progressBar.setVisibility(View.VISIBLE);
        firestoreContacts = new ArrayList<>();
        loadData1();

    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onUserClicked(User user) {
        if(MODE.equals(Constants.MODE_FORWARD)){
            for(Chat forwardChat : forwardChats){
                sendMessage(forwardChat,user);
            }

            mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                    .whereEqualTo(Constants.KEY_SENDER,mine)
                    .whereEqualTo(Constants.KEY_RECEIVER,user.getMobile())
                    .get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    if(task.getResult().getDocuments().size() > 0){
                        conversationId = task.getResult().getDocuments().get(0).getId();
                        updateConversation(forwardChats.get(forwardChats.size()-1).getMessage());
                    }else{

                        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .whereEqualTo(Constants.KEY_SENDER,user.getMobile())
                                .whereEqualTo(Constants.KEY_RECEIVER,mine)
                                .get().addOnCompleteListener(task1 -> {
                            if(task1.isSuccessful()){
                                if(task1.getResult().getDocuments().size() > 0){
                                    conversationId = task1.getResult().getDocuments().get(0).getId();
                                    updateConversation(forwardChats.get(forwardChats.size()-1).getMessage());
                                }else{
                                    HashMap<String, Object> conversation = new HashMap<>();
                                    conversation.put(Constants.KEY_SENDER,mine);
                                    conversation.put(Constants.KEY_RECEIVER,user.getMobile());
                                    conversation.put(Constants.KEY_SENDER_NAME,me.getName());
                                    conversation.put(Constants.KEY_SENDER_PROFILE_URL,me.getProfile_url());
                                    conversation.put(Constants.KEY_RECEIVER_NAME,user.getName());
                                    conversation.put(Constants.KEY_RECEIVER_PROFILE_URL,user.getProfile_url());
                                    conversation.put(Constants.KEY_LAST_MESSAGE,forwardChats.get(forwardChats.size()-1).getMessage());
                                    conversation.put(Constants.KEY_TIMESTAMP,new Date());
                                    conversation.put(Constants.KEY_STATUS,Constants.VALUE_DELIVERED);
                                    addConversation(conversation);
                                }
                            }
                        });
                    }
                }
            });

            startActivity(new Intent(getApplicationContext(),HomeActivity.class));

            //raise a toast for successful execution
            Toast.makeText(this, "Message Forward Successfully!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_UID,user.getMobile());
        startActivity(intent);
        finish();
    }

    public List<String> getContacts(){
        List<String> contacts = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_CONTACTS},0);
        }
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (phones.moveToNext())
        {
            String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            if(phoneNumber.replaceAll("[\\-\\s]","").length() > 10){
                contacts.add(phoneNumber.replaceAll("[\\-\\s]","").substring(3));
            }else{
                contacts.add(phoneNumber.replaceAll("[\\-\\s]",""));
            }
        }
        phones.close();
        return contacts;
    }

    private List<String> getContactsFromSP() {
        List<String> contacts;
        // method to load arraylist from shared prefs
        // initializing our shared prefs with name as
        // shared preferences.
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

        // creating a variable for gson.
        Gson gson = new Gson();

        // below line is to get to string present from our
        // shared prefs if not present setting it as null.
        String json = sharedPreferences.getString(Constants.KEY_CONTACTS, null);

        // below line is to get the type of our array list.
        Type type = new TypeToken<ArrayList<String>>() {}.getType();

        // in below line we are getting data from gson
        // and saving it to our array list
        contacts = gson.fromJson(json, type);

        // checking below if the array list is empty or not
        if (contacts == null) {
            // if the array list is empty
            // creating a new array list.
            contacts = new ArrayList<>();
        }

        return contacts;
    }

    private void saveContacts(List<String> contacts) {
        // method for saving the data in array list.
        // creating a variable for storing data in
        // shared preferences.
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

        // creating a variable for editor to
        // store data in shared preferences.
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (contacts == null){
            editor.putString(Constants.KEY_CONTACTS, null);

        }
        // creating a new variable for gson.
       else {
            Gson gson = new Gson();

            // getting data from gson and storing it in a string.
            String json = gson.toJson(contacts);

            // below line is to save data in shared
            // prefs in the form of string.
            editor.putString(Constants.KEY_CONTACTS, json);
        }

        // below line is to apply changes
        // and save data in shared prefs.
        editor.apply();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendMessage(Chat rChat, User user) {

        HashMap<String, Object> chat = new HashMap<>();
        chat.put(Constants.KEY_MESSAGE, rChat.getMessage());
        chat.put(Constants.KEY_SENDER, mine);
        chat.put(Constants.KEY_RECEIVER, user.getMobile());
        chat.put(Constants.KEY_TIMESTAMP, new Date());
        chat.put(Constants.KEY_STATUS, Constants.VALUE_DELIVERED);
        chat.put(Constants.KEY_IMAGE_URL, rChat.getImage_url());

        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .add(chat);



    }

    private void addConversation(HashMap<String, Object> conversation){
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnCompleteListener(task -> conversationId = task.getResult().getId());
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




}

