package com.whatshide.android;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whatshide.android.utilities.Constants;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView menu_icon;
    private FirebaseAuth mAuth;
    private TextView btnPs,btn1,btn2,btn3,btn4,btn5,btn6,btn7,btn8,btn9,btn0,btnPlus,btnDel,btnMinus,btnMul,btnDivide,btnAns,btnAc;
    private TextView ans,input;
    private String INPUT_STRING = "";
    private String ANS_STRING = "0";
    private String OPCODE = null;
    private boolean PASSWORD_MODE = false;
    private float INPUT = 0;
    private float ANS = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindView();

        setListener();

    }


    private void bindView() {
        mAuth = FirebaseAuth.getInstance();
        btn0 = (TextView) findViewById(R.id.btn_0);
        btn1 = (TextView) findViewById(R.id.btn_1);
        btn2 = (TextView) findViewById(R.id.btn_2);
        btn3 = (TextView) findViewById(R.id.btn_3);
        btn4 = (TextView) findViewById(R.id.btn_4);
        btn5 = (TextView) findViewById(R.id.btn_5);
        btn6 = (TextView) findViewById(R.id.btn_6);
        btn7 = (TextView) findViewById(R.id.btn_7);
        btn8 = (TextView) findViewById(R.id.btn_8);
        btn9 = (TextView) findViewById(R.id.btn_9);
        btnPlus = (TextView) findViewById(R.id.btn_plus);
        btnMinus = (TextView) findViewById(R.id.btn_minus);
        btnMul = (TextView) findViewById(R.id.btn_X);
        btnDivide = (TextView) findViewById(R.id.btn_perc);
        btnAns = (TextView) findViewById(R.id.btn_eq);
        btnPs = (TextView) findViewById(R.id.btn_p);
        btnAc = (TextView) findViewById(R.id.btn_ac);
        btnDel = (TextView) findViewById(R.id.btn_del);
        input = (TextView) findViewById(R.id.input);
        ans = (TextView) findViewById(R.id.answer);


    }


    private void setListener() {
        btn0.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);
        btnPlus.setOnClickListener(this);
        btnPs.setOnClickListener(this);
        btnDivide.setOnClickListener(this);
        btnMinus.setOnClickListener(this);
        btnMul.setOnClickListener(this);
        btnAc.setOnClickListener(this);
        btnAns.setOnClickListener(this);
        btnPs.setOnClickListener(this);
        btnDel.setOnClickListener(this);

    }


    private void updateUi(FirebaseUser currentUser) {
        if(currentUser==null){
            startActivity(new Intent(getApplicationContext(),LoginActivity.class));
        }
        else{
        startActivity(new Intent(getApplicationContext(),HomeActivity.class));
        }
        finish();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btn_0:
                addToInput("0");
                break;
            case R.id.btn_1:
                addToInput("1");
                break;
            case R.id.btn_2:
                addToInput("2");
                break;
            case R.id.btn_3:
                addToInput("3");
                break;
            case R.id.btn_4:
                addToInput("4");
                break;
            case R.id.btn_5:
                addToInput("5");
                break;
            case R.id.btn_6:
                addToInput("6");
                break;
            case R.id.btn_7:
                addToInput("7");
                break;

            case R.id.btn_8:
                addToInput("8");
                break;
            case R.id.btn_9:
                addToInput("9");
                break;
            case R.id.btn_ac:
                INPUT = 0;
                INPUT_STRING = "";
                input.setText(INPUT_STRING);
                ans.setText("0");
                ANS_STRING = "0";
                ANS = 0;
                break;
            case R.id.btn_plus:
                ANS = ANS + INPUT;
                INPUT_STRING += "+";
                OPCODE = "+";
                display();
                break;
            case R.id.btn_minus:
                ANS = ANS - INPUT;
                INPUT_STRING += "-";
                OPCODE = "-";
                display();
                break;
            case R.id.btn_perc:
                INPUT_STRING += "/";

                if(ANS == 0){
                    ANS = 1 / INPUT;
                }
                else {
                    ANS = ANS / INPUT;
                }
                OPCODE = "/";
                display();
                break;
            case R.id.btn_X:
                INPUT_STRING += "*";

                if(ANS == 0){
                    ANS = 1 * INPUT;
                }else {
                    ANS = ANS * INPUT;
                }
                OPCODE = "x";
                display();
                break;
            case R.id.btn_eq:
                submit();
                break;
            case R.id.btn_del:
                INPUT = (float) Math.floor(INPUT / 10);
                if(INPUT_STRING != null && INPUT_STRING.length()>1){
                    INPUT_STRING = INPUT_STRING.substring(0,INPUT_STRING.length()-1);
                }
                else if(INPUT_STRING != null && INPUT_STRING.length() == 1){
                    INPUT_STRING = "";
                }
                input.setText(INPUT_STRING);
                break;
            case R.id.btn_p:
                if(PASSWORD_MODE){
                    PASSWORD_MODE = false;
                }else {
                    PASSWORD_MODE = true;
                }

        }

    }

    private void display() {
        input.setText(INPUT_STRING);
        ANS_STRING = Float.toString(ANS);
        ans.setText(ANS_STRING);
        INPUT = 0;
    }

    private void addToInput(String  i) {
        INPUT_STRING += i;
        INPUT = INPUT * 10 + Float.parseFloat(i);
        input.setText(INPUT_STRING);
    }
    private void submit(){
        if(PASSWORD_MODE){
            checkPassword();
        }
        if(OPCODE != null){
            switch (OPCODE){
                case "+":
                    ANS = ANS + INPUT;
                    break;
                case "-":
                    ANS = ANS - INPUT;
                    break;
                case "/":
                    ANS = ANS / INPUT;
                    break;
                case "x":
                    ANS = ANS * INPUT;
                    break;
                default:
                    break;

            }
        }
        ANS_STRING = Float.toString(ANS);
        ans.setText(ANS_STRING);
        input.setText("");
        INPUT = 0;
        INPUT_STRING = "";

    }

    private void checkPassword() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        String password = sharedPreferences.getString(Constants.KEY_PASSWORD,null);
        if(password == null){
            editor.putString(Constants.KEY_PASSWORD,"1234");
            editor.apply();
        }
        password = sharedPreferences.getString(Constants.KEY_PASSWORD,null);

        if(INPUT_STRING.equals(password)){
            updateUi(mAuth.getCurrentUser());
            return;
        }
    }
}