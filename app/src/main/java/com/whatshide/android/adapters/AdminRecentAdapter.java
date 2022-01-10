package com.whatshide.android.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.whatshide.android.AdminChatActivity;
import com.whatshide.android.R;
import com.whatshide.android.models.Chat;
import com.whatshide.android.utilities.Constants;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminRecentAdapter extends RecyclerView.Adapter<AdminRecentAdapter.myViewHolder> {
    private Context context;
    private List<Chat> list;

    public AdminRecentAdapter(Context context, List<Chat> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_recent_card_layout,null,false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Chat chat = list.get(position);
        holder.users.setText(chat.senderName + " and " + chat.receiverName);
        Glide.with(context)
                .load(chat.senderProfile)
                .into(holder.senderImage);
        Glide.with(context)
                .load(chat.receiverProfile)
                .into(holder.receiverImage);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AdminChatActivity.class);
                intent.putExtra(Constants.KEY_SENDER,chat.getSender());
                intent.putExtra(Constants.KEY_RECEIVER,chat.getReceiver());
                intent.putExtra(Constants.KEY_SENDER_NAME,chat.senderName);
                intent.putExtra(Constants.KEY_RECEIVER_NAME,chat.receiverName);
                intent.putExtra(Constants.KEY_ID,chat.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class myViewHolder extends RecyclerView.ViewHolder{
        private TextView users;
        private CircleImageView senderImage,receiverImage;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.sender_profile);
            receiverImage = itemView.findViewById(R.id.receiver_profile);
            users = itemView.findViewById(R.id.users);
        }
    }
}
