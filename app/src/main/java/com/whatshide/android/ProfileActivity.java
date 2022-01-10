package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {


    private static final int ADMIN_PASSWORD_CHECK = 1;
    private static final String SECURITY_ADMIN_PASSWORD_CHECK_TITLE = "Admin Pin";
    private static final int SCREEN_PASSWORD_CHANGE = 2;
    private static final String SECURITY_SCREEN_PASSWORD_CHANGE = "Change Pin";
    private static final int ADMIN_PASSWORD_CHANGE = 3;
    private static final String ADMIN_PASSWORD_CHANGE_TITLE = "Change Admin Pin";

    CircleImageView profile;
    TextView name,mobileNumber;
    FirebaseAuth mAuth;
    FirebaseFirestore mDatabase;
    RelativeLayout signout,edit_profile,security,admin;
    ProgressBar progressBar;
    ImageView back;
    String mine;

    private int security_container_mode = 0;


    private EditText securityPassword;
    private RelativeLayout securityContainer;
    private TextView securitySubmit,securityTitle;
    private View cover;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        bindView();

        initDB();

        getUserData();

        setListener();
    }

    private void setListener() {
        back.setOnClickListener(backListener);
        signout.setOnClickListener(signOutListener);
        edit_profile.setOnClickListener(editProfileClickListener);
        security.setOnClickListener(securityListener);
        cover.setOnClickListener(coverListener);
        securitySubmit.setOnClickListener(submitListener);
        admin.setOnClickListener(adminListener);
    }

    private View.OnClickListener adminListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            security_container_mode = ADMIN_PASSWORD_CHECK;
            securityTitle.setText(SECURITY_ADMIN_PASSWORD_CHECK_TITLE);
            openSecurityContainer();
        }
    };

    private void openAdminActivity() {
        startActivity(new Intent(getApplicationContext(),AdminHomeActivity.class));
    }

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener signOutListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Sign Out");
            builder.setMessage("Do You Really Want to Do It?");
            builder.setCancelable(true);
            builder.setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAuth.signOut();
                    Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
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
    };

    private View.OnClickListener editProfileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
            intent.putExtra(Constants.KEY_MOBILE,mAuth.getCurrentUser().getPhoneNumber());
            startActivity(intent);
        }
    };

    private void getUserData() {
        mine = mAuth.getCurrentUser().getPhoneNumber();
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(mine)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            User me = task.getResult().toObject(User.class);
                            name.setText(me.getName());
                            Glide.with(getApplicationContext()).load(me.getProfile_url()).into(profile);
                            mobileNumber.setText(me.getMobile());
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private void initDB() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
    }

    private void bindView() {
        profile = (CircleImageView) findViewById(R.id.profile);
        name = (TextView) findViewById(R.id.name);
        mobileNumber = (TextView) findViewById(R.id.mobileNumber);
        signout = (RelativeLayout) findViewById(R.id.signout);
        admin = (RelativeLayout) findViewById(R.id.admin);
        edit_profile = (RelativeLayout) findViewById(R.id.edit_profile);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        security = (RelativeLayout) findViewById(R.id.security);

        securityContainer = (RelativeLayout) findViewById(R.id.security_container);
        securityPassword = (EditText) findViewById(R.id.password);
        securitySubmit = (TextView) findViewById(R.id.security_submit);
        cover = (View) findViewById(R.id.cover);
        securityTitle = (TextView) findViewById(R.id.security_container_title);
        back = (ImageView) findViewById(R.id.back);
    }

    private View.OnClickListener securityListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Actions");
            builder.setMessage("Choose a action");
            builder.setPositiveButton("Set Screen PIN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    security_container_mode = SCREEN_PASSWORD_CHANGE;
                    securityTitle.setText(SECURITY_SCREEN_PASSWORD_CHANGE);
                    openSecurityContainer();
                }
            });
            builder.setNegativeButton("Set Admin PIN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    security_container_mode = ADMIN_PASSWORD_CHANGE;
                    securityTitle.setText(ADMIN_PASSWORD_CHANGE_TITLE);
                    openSecurityContainer();
                }
            });
            android.app.AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    };

    private void openSecurityContainer() {
        securityContainer.setVisibility(View.VISIBLE);
        cover.setVisibility(View.VISIBLE);
        securityContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_open));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in));
    }


    private View.OnClickListener coverListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSecurityContainer();
        }
    };

    private void closeSecurityContainer() {

        securityContainer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.image_close));
        cover.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_out));
        securityPassword.setText(null);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                securityContainer.setVisibility(View.GONE);
                cover.setVisibility(View.GONE);
            }
        },200);
    }

    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(security_container_mode == ADMIN_PASSWORD_CHECK){
                checkAdminPassword();
            }
            else if(security_container_mode == SCREEN_PASSWORD_CHANGE){
                changeScreenLockPassword();
            }
            else if(security_container_mode == ADMIN_PASSWORD_CHANGE){
                changeAdminPassword();
            }
        }
    };

    private void changeAdminPassword() {
        mDatabase.collection(Constants.KEY_COLLECTION_ADMINS)
                .document(mine)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult().exists()){
                            String pin = securityPassword.getText().toString();
                            if(pin.equals(null) && pin.equals("")){
                                return;
                            }
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put(Constants.KEY_PIN,pin);
                            mDatabase.collection(Constants.KEY_COLLECTION_ADMINS)
                                    .document(mine)
                                    .update(hashMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                securityPassword.setText(null);
                                                closeSecurityContainer();
                                                Toast.makeText(ProfileActivity.this, "Admin PIN Changed Successfully!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        else{
                            closeSecurityContainer();
                            Toast.makeText(ProfileActivity.this, "Actions not Authorised. Contact developer", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void checkAdminPassword() {
        String pin = securityPassword.getText().toString();
        if(pin.equals(null) && pin.equals("")){
            return;
        }
        mDatabase.collection(Constants.KEY_COLLECTION_ADMINS)
                .document(mine)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult().exists()){
                            if(pin.equals(task.getResult().getString(Constants.KEY_PIN))){
                                openAdminActivity();
                                securityPassword.setText(null);
                                closeSecurityContainer();
                            }
                            else{
                                Toast.makeText(ProfileActivity.this, "Wrong Pin!", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            closeSecurityContainer();
                            Toast.makeText(ProfileActivity.this, "You are not Authorised to perform this action!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void changeScreenLockPassword() {
        String passwordText = securityPassword.getText().toString();
        if(passwordText.equals("")){
            return;
        }
        if(passwordText.length()!=4){
            Toast.makeText(ProfileActivity.this, "Password length must be 4.", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(Constants.KEY_PASSWORD,passwordText);
        editor.apply();

        securityPassword.setText(null);
        closeSecurityContainer();
        Toast.makeText(ProfileActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();

    }


}