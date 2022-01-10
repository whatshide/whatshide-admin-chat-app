package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.MediaRouteButton;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.whatshide.android.adapters.AdminRecentAdapter;
import com.whatshide.android.models.Chat;
import com.whatshide.android.utilities.Constants;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminHomeActivity extends AppCompatActivity {
    private FirebaseFirestore mDatabase;
    private FirebaseAuth mAuth;
    private androidx.appcompat.widget.Toolbar myToolbar;
    private RecyclerView recyclerView;
    private List<Chat> recentConversations;
    private AdminRecentAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        initDB();

        bindView();

        getChats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }



    private void bindView() {
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);


        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),RecyclerView.VERTICAL,false));
        recyclerView.hasFixedSize();

        recentConversations = new ArrayList<>();
        adapter = new AdminRecentAdapter(getApplicationContext(),recentConversations);
        recyclerView.setAdapter(adapter);
    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.home:
                super.onBackPressed();
                break;
            case R.id.delete_all:
                askForDeleteAll();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askForDeleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete All Chats");
        builder.setMessage("Do you really want this?");
        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAllChats();
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

    private void deleteAllChats() {
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult().getDocuments().size()>0){
                            progressBar.setVisibility(View.VISIBLE);
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                documentSnapshot.getReference().delete();
                            }
                            progressBar.setVisibility(View.INVISIBLE);

                        }
                    }
                });
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult().getDocuments().size()>0){
                            progressBar.setVisibility(View.VISIBLE);
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                documentSnapshot.getReference().delete();
                            }
                            progressBar.setVisibility(View.INVISIBLE);

                        }
                    }
                });
    }


    private void getChats() {
        mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .addSnapshotListener(snapShotListener);
    }

    private EventListener<QuerySnapshot> snapShotListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
            if(error != null){
                return;
            }
            if(value != null){
                for (DocumentChange documentChange : value.getDocumentChanges()){
                    if(documentChange.getType() == DocumentChange.Type.ADDED){
                        Chat chat = new Chat();
                        chat.setId(documentChange.getDocument().getId());
                        chat.dateObj = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        chat.senderName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chat.receiverName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chat.receiverProfile = documentChange.getDocument().getString(Constants.KEY_RECEIVER_PROFILE_URL);
                        chat.senderProfile = documentChange.getDocument().getString(Constants.KEY_SENDER_PROFILE_URL);
                        chat.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                        chat.setSender(documentChange.getDocument().getString(Constants.KEY_SENDER));
                        chat.setReceiver(documentChange.getDocument().getString(Constants.KEY_RECEIVER));
                        recentConversations.add(chat);
                    }
                    else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                        for (int i=0; i<recentConversations.size(); i++){
                            if(documentChange.getDocument()
                                    .getString(Constants.KEY_SENDER)
                                    .equals(recentConversations.get(i)
                                            .getSender()) &&
                                    documentChange.getDocument()
                                            .getString(Constants.KEY_RECEIVER)
                                            .equals(recentConversations.get(i)
                                                    .getReceiver())){
                                recentConversations.get(i).dateObj = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            }
                        }
                    }
                    else if(documentChange.getType() == DocumentChange.Type.REMOVED){
                        for(int i=0;i<recentConversations.size();i++){
                            if(recentConversations.get(i).getId().equals(documentChange.getDocument().getId())){
                                recentConversations.remove(i);
                                adapter.notifyItemRemoved(i);
                            }
                        }
                    }

                }
                Collections.sort(recentConversations,(obj1, obj2) -> obj2.dateObj.compareTo(obj1.dateObj));
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    };

}
