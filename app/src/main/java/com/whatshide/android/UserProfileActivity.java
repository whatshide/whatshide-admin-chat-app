package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import org.w3c.dom.Text;

public class UserProfileActivity extends AppCompatActivity {
    private TextView name;
    private ImageView profile;
    private LinearLayout deleteConversations;
    private Toolbar toolbar;

    private FirebaseFirestore mDatabase;
    private FirebaseAuth mAuth;

    private String your,mine;
    private User you,me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        bindView();

        initDB();

        getIntentData();

        loadData();

        deleteConversations.setOnClickListener(deleteConListener);
    }

    private View.OnClickListener deleteConListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(UserProfileActivity.this);
            builder.setTitle("Delete All Conversations");
            builder.setMessage("Do you really want to do this?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteConversation();
                    Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    };

    private void deleteConversation() {
        mDatabase.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER,mine)
                .whereEqualTo(Constants.KEY_RECEIVER,your)
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
                .whereEqualTo(Constants.KEY_SENDER,your)
                .whereEqualTo(Constants.KEY_RECEIVER,mine)
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
                .whereEqualTo(Constants.KEY_SENDER,mine)
                .whereEqualTo(Constants.KEY_RECEIVER,your)
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
                .whereEqualTo(Constants.KEY_SENDER,your)
                .whereEqualTo(Constants.KEY_RECEIVER,mine)
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

    private void loadData() {
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(your)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            you = task.getResult().toObject(User.class);
                            setPageData();
                        }
                    }
                });
    }

    private void setPageData() {
        name.setText(you.getName());
        Glide.with(getApplicationContext())
                .load(you.getProfile_url())
                .into(profile);
        toolbar.setTitle(you.getName());

    }

    private void getIntentData() {
        your = getIntent().getStringExtra(Constants.KEY_MOBILE);
    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mine = mAuth.getCurrentUser().getPhoneNumber();
    }

    private void bindView() {
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        name = (TextView) findViewById(R.id.name);
        profile = (ImageView) findViewById(R.id.profile);
        deleteConversations = (LinearLayout) findViewById(R.id.delete_conversations);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}