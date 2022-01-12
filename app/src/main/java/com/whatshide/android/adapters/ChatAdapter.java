package com.whatshide.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whatshide.android.R;
import com.whatshide.android.listeners.ImageMessageListener;
import com.whatshide.android.listeners.MessageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.utilities.Constants;
import com.whatshide.android.utilities.UtilFun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.myViewHolder> {
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    private static final int MSG_TYPE_RIGHT_IMAGE = 3;
    private static final int MSG_TYPE_LEFT_IMAGE = 4;
    private MessageListener messageListener;
    private FirebaseFirestore mDatabase = FirebaseFirestore.getInstance();
    List<Chat> list;
    Context context;
    FirebaseUser firebaseUser;
    private ImageMessageListener imageMessageListener;
    private boolean selectionMode = false;
    public ChatAdapter(List<Chat> list, Context context, ImageMessageListener imageMessageListener, MessageListener messageListener) {
        this.list = list;
        this.context = context;
        this.imageMessageListener = imageMessageListener;
        this.messageListener = messageListener;

    }

    @NonNull
    @Override
    public ChatAdapter.myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == MSG_TYPE_LEFT){
            View v = LayoutInflater.from(context).inflate(R.layout.left_chat,null,false);
            return new ChatAdapter.myViewHolder(v);
        }else if(viewType == MSG_TYPE_RIGHT){
            View v = LayoutInflater.from(context).inflate(R.layout.right_chat,null,false);
            return new ChatAdapter.myViewHolder(v);
        }else if(viewType == MSG_TYPE_LEFT_IMAGE){
            View v = LayoutInflater.from(context).inflate(R.layout.left_image_chat_layout,null,false);
            return new ChatAdapter.myViewHolder(v);
        }else {
            View v = LayoutInflater.from(context).inflate(R.layout.right_image_chat_layout,null,false);
            return new ChatAdapter.myViewHolder(v);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Chat chat = list.get(position);
        holder.msg.setText(chat.getMessage());

        holder.itemView.setLongClickable(true);
        holder.itemView.setSelected(chat.isSelected());

        holder.time.setText(chat.getTime().substring(19));
        if( position>0){
            try {
                if(getDate(chat.dateObj).compareTo(getDate(list.get(position-1).dateObj)) > 0){
                    holder.date.setVisibility(View.VISIBLE);
                    holder.date.setText(chat.dateObj.toString().substring(4,10)+" "+chat.dateObj.toString().substring(30));

                }else {
                    holder.date.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else{
            holder.date.setVisibility(View.VISIBLE);
            holder.date.setText(chat.dateObj.toString().substring(4,10)+" "+chat.dateObj.toString().substring(30));

        }

        if(chat.getImage_url() != null){
            Bitmap original = UtilFun.getBitmapFromEncodeImage(chat.getImage_url());
            Bitmap compressed = UtilFun.getResizedBitmap(original,800,800*original.getHeight()/original.getWidth());
            holder.image.setImageBitmap(compressed);

        }
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //initial press
                selectionMode = !selectionMode;
                chat.setSelected(!chat.isSelected());

                //set selection for first
                messageListener.onMessageSelect(chat);
                notifyItemChanged(position);
                return true;
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chat.getImage_url() != null){
                    imageMessageListener.onImageMessageClicked(chat);
                }
                if(selectionMode){
                    if (!chat.isSelected()) {
                        messageListener.onMessageSelect(chat);
                    }else {
                        messageListener.onMessageRemoved(chat);
                    }
                    chat.setSelected(!chat.isSelected());
                    notifyItemChanged(position);
                }
            }
        });
    }


    public void setSelected(boolean selected){
        if(!selected){
            for(Chat chat:list){
                chat.setSelected(false);
            }
            selectionMode = false;
            notifyDataSetChanged();
        }
    }


    @Override
    public int getItemCount() {
        return list.size();
    }
    public class myViewHolder extends RecyclerView.ViewHolder{
        TextView msg,time,date;
        ImageView image;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            msg = itemView.findViewById(R.id.msg);
            time = itemView.findViewById(R.id.time);
            image = itemView.findViewById(R.id.image);
            date = itemView.findViewById(R.id.date);


        }
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser.getPhoneNumber().toString().equals(list.get(position).getSender()) && list.get(position).getImage_url() != null){
            return MSG_TYPE_RIGHT_IMAGE;
        }
        else if(firebaseUser.getPhoneNumber().toString().equals(list.get(position).getSender())){
            return MSG_TYPE_RIGHT;
        }
        else if(list.get(position).getImage_url()!=null){
            return MSG_TYPE_LEFT_IMAGE;
        }else{
            return MSG_TYPE_LEFT;
        }
    }


    private Date getDate(Date dateObj) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return formatter.parse(formatter.format(dateObj));
    }
}
