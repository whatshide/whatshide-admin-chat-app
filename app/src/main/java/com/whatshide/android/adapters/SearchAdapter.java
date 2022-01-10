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
import com.whatshide.android.listeners.UserListener;
import com.whatshide.android.models.User;


import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.myViewHolder> {
    private List<User> list;
    private Context context;
    private List<User> filtered;
    private final UserListener userListener;

    public SearchAdapter(List<User> list, Context context, UserListener userListener) {
        this.list = list;
        this.context = context;
        this.filtered = list;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.user_search_layout,null,false);
        return new myViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        User user = filtered.get(position);
        holder.phone.setText(user.getMobile());
        holder.name.setText(user.getName());
        Glide.with(context).load(user.getProfile_url()).into(holder.profile);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userListener.onUserClicked(user);
            }
        });
    }



    @Override
    public int getItemCount() {
        return filtered.size();
    }

    public class myViewHolder extends RecyclerView.ViewHolder{
        CircleImageView profile;
        TextView name,phone;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profile);
            name = itemView.findViewById(R.id.name);
            phone = itemView.findViewById(R.id.phone);

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
                    List<User> newFilteredList = new ArrayList<User>();
                    for(User user:list){
                        if(user.getName().toLowerCase().contains(key.toLowerCase())){
                            newFilteredList.add(user);
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
                filtered = (List<User>) results.values;
                notifyDataSetChanged();
            }
        };
    }


}
