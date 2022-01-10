package com.whatshide.android.Notification;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whatshide.android.R;
import com.whatshide.android.utilities.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFunctions {
    public static FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    public static FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    public static APIService apiService;

    public static void sendNotification(Context context, String receiver, String title, String message, String sender) {
        apiService = Client.getClient().create(APIService.class);

        firebaseFirestore.collection(Constants.KEY_COLLECTION_TOKENS)
                .document(receiver)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult().exists()){
                            Token token = task.getResult().toObject(Token.class);
                            Data data = new Data(firebaseAuth
                                    .getCurrentUser()
                                    .getPhoneNumber(),
                                    message,
                                    title,
                                    "WEk7CeqAYnh8s2IFppWAJxkSkWH2",
                                    R.mipmap.ic_launcher);
                            Sender sender = new Sender(data,token.getToken());
                            apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if(response.code() == 200){
                                        Log.d("hey", "working ");
                                        if(response.body().success!= 1){
                                            Log.d("sendNotification", "it is not working ");
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Log.d("sendNotification", "not working ");
                                }
                            });

                            }
                        else{
                            Log.d("task", "onComplete: unsuccessful");
                        }
                    }
                });
    }
}
