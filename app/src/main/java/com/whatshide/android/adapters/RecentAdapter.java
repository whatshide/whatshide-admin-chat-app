package com.whatshide.android.adapters;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.whatshide.android.ChatActivity;
import com.whatshide.android.R;
import com.whatshide.android.listeners.ProfileImageListener;
import com.whatshide.android.models.Chat;
import com.whatshide.android.utilities.Constants;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.myViewHolder> {
    private List<Chat> list;
    private Context context;
    private List<Chat> filtered;
    private final ProfileImageListener profileImageListener;

    public RecentAdapter(List<Chat> list, Context context, ProfileImageListener profileImageListener) {
        this.list = list;
        this.context = context;
        this.filtered = list;
        this.profileImageListener = profileImageListener;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.user_recent_layout,null,false);
        return new myViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Chat chat = filtered.get(position);
        holder.name.setText(chat.conversationName);
        holder.recent.setText(chat.getMessage());
        Glide.with(context).load(chat.conversationImageUrl).into(holder.profile);
        if((chat.dateObj.toString().substring(4,10)+chat.dateObj.toString().substring(30)).compareTo(new Date().toString().substring(4,10)+new Date().toString().substring(30)) == 0){
            holder.time.setText(chat.dateObj.toString().substring(11,16));
        }else{
            holder.time.setText(chat.dateObj.toString().substring(4,10)+" "+chat.dateObj.toString().substring(30));
        }
        holder.profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileImageListener.onProfileClicked(chat);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context,ChatActivity.class);
                intent.putExtra(Constants.KEY_UID,chat.conversationId);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }



    @Override
    public int getItemCount() {
        return filtered.size();
    }

    public class myViewHolder extends RecyclerView.ViewHolder{
        CircleImageView profile;
        TextView name,recent,time;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profile);
            name = itemView.findViewById(R.id.name);
            recent = itemView.findViewById(R.id.recentMessage);
            time = itemView.findViewById(R.id.time);
        }
    }

    public Filter getFilter(){
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String key = constraint.toString();
                if(key.isEmpty()){
                    filtered = list;
                }
                else {
                    List<Chat> newFilteredList = new ArrayList<>();
                    for(Chat chat:list){
                        if(chat.conversationName.toLowerCase().contains(key.toLowerCase())){
                            newFilteredList.add(chat);
                        }
                    }
                    filtered = newFilteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filtered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered = (List<Chat>) results.values;
                notifyDataSetChanged();
            }
        };
    }


}
