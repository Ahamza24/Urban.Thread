package com.urbanthreads.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class UploadFragment extends Fragment {

    private ImageView imagePreview;
    private EditText etTitle, etCategory, etTags;
    private Button btnSelectImage, btnUpload;
    private android.widget.ProgressBar progressBar;

    private Uri imageUri;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imagePreview.setImageURI(uri);
                }
            });

    public UploadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        imagePreview = view.findViewById(R.id.imagePreview);
        etTitle = view.findViewById(R.id.etTitle);
        etCategory = view.findViewById(R.id.etCategory);
        etTags = view.findViewById(R.id.etTags);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        btnUpload = view.findViewById(R.id.btnUpload);
        progressBar = view.findViewById(R.id.progressBar);

        // Using standard initialization to let Firebase SDK handle the bucket from google-services.json
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnSelectImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnUpload.setOnClickListener(v -> uploadDesign());

        return view;
    }

    private void uploadDesign() {
        if (imageUri == null) {
            Toast.makeText(getContext(), "Select image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String tagsString = etTags.getText().toString().trim();

        if (title.isEmpty() || category.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.List<String> tags = new java.util.ArrayList<>();
        if (!tagsString.isEmpty()) {
            String[] splitTags = tagsString.split(",");
            for (String tag : splitTags) {
                tags.add(tag.trim().toLowerCase());
            }
        }

        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String fileName = "designs/" + UUID.randomUUID().toString();
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            saveToFirestore(uri.toString(), title, category, tags);
                        }))
                .addOnFailureListener(e -> {
                    btnUpload.setEnabled(true);
                    btnUpload.setText("Upload Design");
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String imageUrl, String title, String category, java.util.List<String> tags) {
        String designId = UUID.randomUUID().toString();
        String tailorId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

        Design design = new Design(designId, imageUrl, title, tailorId, category, tags);

        db.collection("designs").document(designId)
                .set(design)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Uploaded Successfully!", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    btnUpload.setEnabled(true);
                    btnUpload.setText("Upload Design");
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
