package com.example.mhqltt;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class ActivityHidden extends AppCompatActivity {
    private static final int REQUEST_IMAGE_HIDDEN = 2;
    private static final int REQUEST_IMAGE_CLEAR = 3;
    private static final int REQUEST_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden);

        Button hiddenButton = findViewById(R.id.hidden_button);
        hiddenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndRequestPermissions(REQUEST_IMAGE_HIDDEN);
            }
        });

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndRequestPermissions(REQUEST_IMAGE_CLEAR);
            }
        });
    }

    private void checkAndRequestPermissions(int requestCode) {
        // Kiểm tra quyền truy cập
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Yêu cầu quyền truy cập nếu chưa được cấp
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
        } else {
            // Nếu đã có quyền, mở trình chọn ảnh
            openImagePicker(requestCode);
        }
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                handleImageSelection(requestCode, selectedImageUri);
            } else {
                Toast.makeText(this, "Failed to retrieve image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMessageInputDialog(final Uri imageUri, final int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Message");

        // Thiết lập một EditText để người dùng nhập thông điệp
        final EditText input = new EditText(this);
        builder.setView(input);

        // Nút OK
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String message = input.getText().toString();
                if (requestCode == REQUEST_IMAGE_HIDDEN) {
                    try {
                        Steganography.hideMessageInImage(getContentResolver(), imageUri, message);
                        Toast.makeText(ActivityHidden.this, "Message hidden successfully", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(ActivityHidden.this, "Error hiding message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == REQUEST_IMAGE_CLEAR) {
                    // Không sử dụng trong trường hợp REQUEST_IMAGE_CLEAR
                }
            }
        });

        // Nút Cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private boolean isPng(Uri uri) {
        String extension = "";
        String scheme = uri.getScheme();
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            String path = uri.getPath();
            extension = MimeTypeMap.getFileExtensionFromUrl(path);
        }
        return "png".equalsIgnoreCase(extension);
    }

    private void handleImageSelection(int requestCode, Uri selectedImageUri) {
        if (selectedImageUri != null) {
            if (isPng(selectedImageUri)) {
                if (requestCode == REQUEST_IMAGE_HIDDEN) {
                    showMessageInputDialog(selectedImageUri, requestCode);
                } else if (requestCode == REQUEST_IMAGE_CLEAR) {
                    try {
                        String message = Steganography.extractHiddenMessage(getContentResolver(), selectedImageUri);
                        Toast.makeText(this, "Extracted message: " + message, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error extracting message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "The selected image is not in PNG format.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to get real path from URI", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, tiếp tục xử lý
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
