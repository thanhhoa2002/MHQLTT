package com.example.mhqltt;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageDialogRecovery extends Dialog {

    private Bitmap image;
    private Uri imageUri;

    public ImageDialogRecovery(Context context, Bitmap image, Uri imageUri) {
        super(context);
        this.image = image;
        this.imageUri = imageUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_recovery);

        ImageView imageView = findViewById(R.id.dialogImageView);
//        imageView.setImageBitmap(image);
        Glide.with(getContext()).load(imageUri).into(imageView);
    }
}
