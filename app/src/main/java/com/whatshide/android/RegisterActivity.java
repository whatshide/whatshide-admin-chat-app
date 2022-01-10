package com.whatshide.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    EditText name;
    private Uri image_url = null;
    Button done;
    CircleImageView profile;
    String mobile;
    FirebaseFirestore mDatabase;
    FirebaseStorage firebaseStorage;
    private StorageTask uploadTask;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        bindView();

        initDB();

        getIntentInfo();

        getUserInfo();



        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameValue = name.getText().toString();
                if(!isValid(nameValue)){
                    return;
                }

                if(image_url==null){
                    HashMap<String,Object> hashMap = new HashMap<>();
                    hashMap.put(Constants.KEY_NAME,nameValue);
                    mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                            .document(mobile)
                            .update(hashMap);
                    updateUiToHome();
                }
                else{
                    updateImageName(nameValue);
                }

            }
        });

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
    }

    private void updateImageName(String name) {
        if(image_url==null){
            return;
        }
        StorageReference fileRef = firebaseStorage.getReference("profile_image").
                child(mobile+"profile"+"."+getFileExtension(image_url));

        //compress image
        byte[] data = compressImage(image_url);


        uploadTask = fileRef.putBytes(data).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
                Log.d("upload issue", "onFailure: "+e);
                Toast.makeText(RegisterActivity.this, "hwdh", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String url =uri.toString();
                        HashMap<String,Object> user = new HashMap<>();
                        user.put(Constants.KEY_PROFILE_URL,url);
                        user.put(Constants.KEY_NAME,name);
                        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                                .document(mobile)
                                .update(user)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            mDatabase.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                                    .whereEqualTo(Constants.KEY_SENDER,mobile)
                                                    .whereEqualTo(Constants.KEY_RECEIVER,mobile)
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                            if(task.isSuccessful() && task.getResult().getDocuments().size()>0){
                                                                for(DocumentSnapshot documentSnapshot:task.getResult()){
                                                                    HashMap<String, Object> hashMap = new HashMap<>();
                                                                    if(documentSnapshot.getString(Constants.KEY_SENDER).equals(mobile)){
                                                                        hashMap.put(Constants.KEY_SENDER_NAME,name);
                                                                        hashMap.put(Constants.KEY_SENDER_PROFILE_URL,url);
                                                                        documentSnapshot.getReference().update(hashMap);
                                                                    }else if(documentSnapshot.getString(Constants.KEY_RECEIVER).equals(mobile)){
                                                                        hashMap.put(Constants.KEY_RECEIVER_NAME,name);
                                                                        hashMap.put(Constants.KEY_RECEIVER_PROFILE_URL,url);
                                                                        documentSnapshot.getReference().update(hashMap);
                                                                    }
                                                                }
                                                                updateUiToHome();
                                                                finish();
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(RegisterActivity.this, "upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void getIntentInfo() {
        mobile = getIntent().getStringExtra(Constants.KEY_MOBILE);
    }

    private void bindView() {
        name = (EditText) findViewById(R.id.name);
        done = (Button) findViewById(R.id.done);
        profile = (CircleImageView) findViewById(R.id.profile);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void initDB() {
        mDatabase = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void updateUiToHome() {
        Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private byte[] compressImage(Uri image_url) {
        Bitmap bmp = null;
        try {
            bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), image_url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos);
        return baos.toByteArray();
    }

    private boolean isValid(String nameValue) {
        if(nameValue.equals("")){
            name.setError("name is required!");
            name.requestFocus();
            return false;
        }else{
            return true;
        }
    }

    private void getUserInfo() {
        mDatabase.collection(Constants.KEY_COLLECTION_USERS)
                .document(mobile)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            User user = task.getResult().toObject(User.class);
                            name.setText(user.getName());
                            Glide.with(getApplicationContext()).load(user.getProfile_url()).into(profile);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    private void openFileChooser() {

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(i,PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            image_url = data.getData();

            Glide.with(getApplicationContext()).load(image_url).into(profile);
        }
    }


    private String getFileExtension(Uri image_url) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(image_url));
    }
}