package com.example.mhqltt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private Context mContext;
    private List<Bitmap> mImages;
    private List<DirectoryEntry> mEntries;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnItemClickListener {
        void onItemClick(Bitmap image, DirectoryEntry entry);
    }

    private OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public ImageAdapter(Context context, List<Bitmap> images, List<DirectoryEntry> entries) {
        mContext = context;
        mImages = images;
        mEntries = entries;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Bitmap image = mImages.get(position);
        Glide.with(mContext).load(image).into(holder.imageView);
        holder.tickView.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mImages.size();
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView tickView;

        public ImageViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tickView = itemView.findViewById(R.id.tickView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(((BitmapDrawable) imageView.getDrawable()).getBitmap(), mEntries.get(position));
                            notifyItemChanged(selectedPosition);
                            selectedPosition = getLayoutPosition();
                            notifyItemChanged(selectedPosition);
                        }
                    }
                }
            });
        }
    }
}
