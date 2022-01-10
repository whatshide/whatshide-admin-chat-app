package com.whatshide.android.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.whatshide.android.R;

import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.myViewHolder> {
    private List<Uri> list;
    private Context context;

    public ImageListAdapter(List<Uri> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_card_layout,null,false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        Uri imageUri = list.get(position);
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class myViewHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
        }
    }
}
