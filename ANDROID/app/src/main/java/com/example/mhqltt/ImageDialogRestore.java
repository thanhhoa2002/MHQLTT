package com.example.mhqltt;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageDialogRestore extends Dialog {

    private Bitmap image;

    public ImageDialogRestore(Context context, Bitmap image) {
        super(context);
        this.image = image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_restore);

        ImageView imageView = findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(image);
    }
}
