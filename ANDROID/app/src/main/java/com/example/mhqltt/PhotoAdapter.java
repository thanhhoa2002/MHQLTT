package com.example.mhqltt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private Context context;
    private List<Uri> photoUris;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Bitmap bitmap, Uri imageUri);
    }

    public PhotoAdapter(Context context, List<Uri> photoUris) {
        this.context = context;
        this.photoUris = photoUris;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updatePhotoUris(List<Uri> photoUris) {
        this.photoUris = photoUris;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri photoUri = photoUris.get(position);

        Glide.with(context)
                .load(photoUri)
                .centerCrop()
                .into(holder.imageView);

        holder.tickView.setVisibility(selectedPositions.contains(position) ? View.VISIBLE : View.GONE);

        holder.imageView.setOnClickListener(v -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
                holder.tickView.setVisibility(View.GONE);
            } else {
                selectedPositions.add(position);
                holder.tickView.setVisibility(View.VISIBLE);

                Bitmap bitmap = ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap();
                if (listener != null) {
                    listener.onItemClick(bitmap, photoUri);
                }
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView tickView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tickView = itemView.findViewById(R.id.tickView);
        }
    }
}
