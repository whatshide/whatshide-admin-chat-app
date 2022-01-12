package com.whatshide.android.utilities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

public class CustomeURLSpan extends URLSpan {

    public CustomeURLSpan(String url) {
        super(url);
    }

    public CustomeURLSpan(@NonNull Parcel src) {
        super(src);
    }

    @Override
    public void onClick(View widget) {
        super.onClick(widget);
        Uri uri = Uri.parse(getURL());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
    }
}
