package com.whatshide.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.whatshide.android.AdminChatActivity;
import com.whatshide.android.R;
import com.whatshide.android.listeners.ImageMessageListener;
import com.whatshide.android.listeners.MessageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.models.User;
import com.whatshide.android.utilities.UtilFun;

import java.util.List;

public class AdminChatAdapter extends RecyclerView.Adapter<AdminChatAdapter.myViewHolder> {
    private static final int MSG_TYPE_IMAGE = 0;
    private static final int MSG_TYPE_TEXT = 1;
    private Context context;
    private List<Chat> list;
    private String sender,receiver,senderName,receiverName;
    private MessageListener messageListener;
    private ImageMessageListener imageMessageListener;
    private int selectedChat = RecyclerView.NO_POSITION;
    public AdminChatAdapter(Context context, List<Chat> list, String sender, String senderName, String receiver, String receiverName, ImageMessageListener imageMessageListener , MessageListener messageListener) {
        this.context = context;
        this.list = list;
        this.sender = sender;
        this.senderName = senderName;
        this.receiver = receiver;
        this.receiverName = receiverName;
        this.messageListener = messageListener;
        this.imageMessageListener = imageMessageListener;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == MSG_TYPE_IMAGE){
            view = LayoutInflater.from(context).inflate(R.layout.admin_chat_card_image_layout,parent,false);

        }else{
            view = LayoutInflater.from(context).inflate(R.layout.admin_chat_card_layout,parent,false);

        }
        return new myViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Chat chat = list.get(position);
        holder.sender.setText(getName(chat.getSender()));
        holder.message.setText(chat.getMessage());
        holder.time.setText(chat.getTime());
        holder.itemView.setSelected(position == selectedChat);
        if(chat.getImage_url() != null){
            Bitmap original = UtilFun.getBitmapFromEncodeImage(chat.getImage_url());
            Bitmap compressed = UtilFun.getResizedBitmap(original,800,800*original.getHeight()/original.getWidth());
            holder.image.setImageBitmap(compressed);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chat.getImage_url() != null){
                    imageMessageListener.onImageMessageClicked(chat);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                messageListener.onMessageSelect(chat);
                notifyItemChanged(selectedChat);
                selectedChat = position;
                notifyItemChanged(position);
                return true;
            }
        });

    }

    public void setSelected(boolean selected){
        if(!selected){
            int pos = selectedChat;
            selectedChat = RecyclerView.NO_POSITION;
            notifyItemChanged(pos);

        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public String getName(String number){
        if(number.equals(sender)){
            return senderName;
        }else{
            return receiverName;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(list.get(position).getImage_url() != null){
            return MSG_TYPE_IMAGE;
        }else{
            return MSG_TYPE_TEXT;
        }
    }

    public class myViewHolder extends RecyclerView.ViewHolder{
        private TextView sender, message, time;
        private ImageView image;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.sender_name);
            message = itemView.findViewById(R.id.message);
            time = itemView.findViewById(R.id.time);
            image = itemView.findViewById(R.id.image);
        }
    }
}
