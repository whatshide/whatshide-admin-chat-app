package com.whatshide.android.Notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Authorization:key=AAAAs1ohuFU:APA91bG4q-zldNSLSTMPQIQhnIE5aJcn0ZvzK33wILk4OH1QL_PPgEPR_spy6Av4G6EzRaEPDPCw9DWnJ5iQsQEbZyXvm4sWpHJ_FGzi7fngDKaNKaGyl-vibznkuQVWk5CmArSTMBx8",
                    "Content-Type:application/json"

            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
