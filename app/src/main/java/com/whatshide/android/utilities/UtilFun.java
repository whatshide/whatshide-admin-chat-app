package com.whatshide.android.utilities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class UtilFun {
    public static FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    public static FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

   public static void setStatus(String status,String mobile){
       HashMap<String ,Object> hashMap = new HashMap<>();
       hashMap.put(Constants.KEY_STATUS,status);
       firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
               .document(mobile)
               .update(hashMap)
               .addOnCompleteListener(new OnCompleteListener<Void>() {
                   @Override
                   public void onComplete(@NonNull Task<Void> task) {
                       if(task.isSuccessful()){
                           Log.d("status", "onComplete: status updated successfully");
                       }else{
                           Log.d("status", "onComplete: "+task.getException());
                       }
                   }
               });
   }

   public static void setLastSeen(String user){
       HashMap<String,Object> hashMap = new HashMap<>();
       hashMap.put(Constants.KEY_LAST_SEEN,getCurrentTime());
       firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
               .document(user)
               .update(hashMap);
   }
   public static String getCurrentTime(){
       return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
   }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap getBitmapFromEncodeImage(String image_url) {
        byte[] bytes = Base64.getDecoder().decode(image_url);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        return bitmap;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static int getQualityFactor(int size){
       if(size < 1000000){
            return 25;
       }
       else if(size >= 1000000 && size < 5000000){
            return 20;
       }
       else if(size >= 5000000 && size < 10000000){
           return 15;
       }
       else if(size >= 10000000 && size < 50000000){
           return 10;
       }
       else if(size > 50000000){
           return 7;
       }
       return 10;
    }
}
