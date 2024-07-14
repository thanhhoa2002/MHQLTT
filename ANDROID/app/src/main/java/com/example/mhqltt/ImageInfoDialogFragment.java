package com.example.mhqltt;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
public class ImageInfoDialogFragment extends DialogFragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_EXTENSION = "extension";
    private static final String ARG_CREATION_DATE = "creation_date";

    public static ImageInfoDialogFragment newInstance(String name, String extension, String creationDate) {
        ImageInfoDialogFragment fragment = new ImageInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_EXTENSION, extension);
        args.putString(ARG_CREATION_DATE, creationDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image_info, container, false);

        TextView nameTextView = view.findViewById(R.id.textViewName);
        TextView extensionTextView = view.findViewById(R.id.textViewExtension);
        TextView creationDateTextView = view.findViewById(R.id.textViewCreationDate);

        if (getArguments() != null) {
            String name = getArguments().getString(ARG_NAME);
            String extension = getArguments().getString(ARG_EXTENSION);
            String creationDate = getArguments().getString(ARG_CREATION_DATE);

            nameTextView.setText(name);
            extensionTextView.setText(extension);
            creationDateTextView.setText(creationDate);
        }

        return view;
    }
}
