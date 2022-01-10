package com.whatshide.android;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import java.util.concurrent.TimeUnit;

public class VerificationActivity extends AppCompatActivity {
    EditText otp;
    Button done;
    TextView notice;
    private FirebaseAuth mAuth;
    FirebaseFirestore mDatabase;
    String verificationCodeBySystem;
    String mobile;
    User user;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        bindView();

        initDB();

        getUserDataFromIntent();

        // set notice
        notice.setText("Please Enter The OTP sent on this number "+mobile);

        //send Verification code to number
        sendVerificationCode();



        //verify otp with done click
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if valid or not
                if(!isValid(otp)){
                    return;
                }

                //verify the otp
               verifyCode(otp.getText().toString());

               //updateUi(mobileNumber);
            }

        });
    }

    private void alert(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void getUserDataFromIntent() {
        mobile = getIntent().getStringExtra(Constants.KEY_MOBILE);
    }

    private void bindView() {
        notice = (TextView) findViewById(R.id.notice);
        done = (Button) findViewById(R.id.done);
        otp = (EditText) findViewById(R.id.otp);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private boolean isValid(EditText otp) {
        String code = otp.getText().toString();
        if(code.equals("")){
            otp.setError("Otp required!");
            otp.requestFocus();
            return false;
        }else{
            return true;
        }
    }

    private void updateUi() {
        Intent intent = new Intent(getApplicationContext(),RegisterActivity.class);
        intent.putExtra(Constants.KEY_MOBILE,mobile);
        startActivity(intent);
        finish();
    }

    private void sendVerificationCode() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(mobile)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            verificationCodeBySystem = s;
            progressBar.setVisibility(View.INVISIBLE);

        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();
            if(code!=null){
                verifyCode(code);
            }
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(VerificationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private void verifyCode(String code) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCodeBySystem,code);
        signInTheUserByCredentials(credential);
    }

    private void signInTheUserByCredentials(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(VerificationActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                   checkUserAndUpdate();

                }else{
                    Toast.makeText(VerificationActivity.this, "Error Occurred! Try again later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkUserAndUpdate() {
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(mobile)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && !task.getResult().exists()){
                            User user = new User();
                            user.setName("guest"+Math.random());
                            user.setProfile_url("https://firebasestorage.googleapis.com/v0/b/whats-hide.appspot.com/o/profile_image%2Frandom_profile.png?alt=media&token=3cc46a49-48bc-4cb6-8639-a3b90449cdfc");
                            user.setMobile(mobile);
                            user.setStatus("offline");
                            user.setLastSeen("00:00");
                            mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(mobile)
                                    .set(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                alert("Sign In Successful");
                                            }else {
                                                alert("Sign In Unsuccessful");
                                            }
                                        }
                                    });
                        }
                        updateUi();
                    }
                });
    }


}