package com.whatshide.android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.whatshide.android.utilities.Constants;

public class LoginActivity extends AppCompatActivity {
    Button done;
    EditText mobile;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mobile = (EditText) findViewById(R.id.mobileNumber);
        done = (Button) findViewById(R.id.done);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mobile.getText().toString();
                progressBar.setVisibility(View.VISIBLE);
                if(!isValid(phoneNumber)){
                    return;
                }
                else{
                    updateUi(phoneNumber);
                }
            }
        });
    }

    private boolean isValid(String  phoneNumber) {
        if(phoneNumber.equals("")) {
            mobile.requestFocus();
            mobile.setError("Mobile Number is Required!");
            return false;
        }else{
            return true;
        }
    }


    private void updateUi(String phoneNumber) {
        Intent intent = new Intent(getApplicationContext(),VerificationActivity.class);
        intent.putExtra(Constants.KEY_MOBILE,"+91"+phoneNumber);
        startActivity(intent);
    }
}