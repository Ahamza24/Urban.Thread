package com.urbanthreads.app;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class UploadFragment extends Fragment {
    private static final String TAG = "UploadFragment";
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private ImageView imagePreview;
    private EditText etTitle, etDescription, etTags;
    private Button btnSelectImage, btnUpload;
    private android.widget.ProgressBar progressBar;

    private Uri imageUri;

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
        etDescription = view.findViewById(R.id.etDescription);
        etTags = view.findViewById(R.id.etTags);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        btnUpload = view.findViewById(R.id.btnUpload);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnSelectImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnUpload.setOnClickListener(v -> uploadDesign());

        return view;
    }

    private void uploadDesign() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login before uploading a design", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(getContext(), "Select image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String tagsString = etTags.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
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

        String contentType = requireContext().getContentResolver().getType(imageUri);
        if (contentType == null || !contentType.startsWith("image/")) {
            Toast.makeText(getContext(), "Please select a valid image file", Toast.LENGTH_SHORT).show();
            return;
        }

        setUploadInProgress(true);
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    models.User user = documentSnapshot.toObject(models.User.class);
                    if (user == null || !"tailor".equals(user.getRole())) {
                        resetUploadState("Only tailor accounts can upload designs.");
                        return;
                    }

                    uploadImageBytes(currentUser.getUid(), contentType, title, description, tags);
                })
                .addOnFailureListener(e -> resetUploadState("Could not verify your account: " + e.getMessage()));
    }

    private void uploadImageBytes(
            String userId,
            String contentType,
            String title,
            String description,
            java.util.List<String> tags
    ) {
        byte[] imageBytes;
        try {
            imageBytes = readImageBytes(imageUri);
        } catch (IOException e) {
            resetUploadState("Could not read selected image: " + e.getMessage());
            return;
        }

        String fileName = "designs/" + userId + "/" + UUID.randomUUID() + getFileExtension(imageUri);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(contentType)
                .setCustomMetadata("ownerUid", userId)
                .build();
        // Explicitly defining the bucket to avoid 404 errors caused by domain mismatches
        String bucketUrl = "gs://urban-threads-13a11.firebasestorage.app";
        StorageReference fileRef = FirebaseStorage.getInstance(bucketUrl).getReference().child(fileName);

        fileRef.putBytes(imageBytes, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                    }
                    return task.getResult().getStorage().getDownloadUrl();
                })
                .addOnSuccessListener(uri -> saveToFirestore(uri.toString(), title, description, tags))
                .addOnFailureListener(e -> {
                    if (isObjectNotFound(e)) {
                        resetUploadState("Firebase Storage is not available for this project yet.\n\n"
                                + "Open Firebase Console, enable Storage, and create the default bucket for urban-threads-13a11.\n\n"
                                + "Path attempted: " + fileRef.getPath());
                        return;
                    }

                    resetUploadState("Upload failed\n\nPath: " + fileRef.getPath()
                            + "\n\nError: " + formatStorageError(e));
                });
    }

    private void saveToFirestore(String imageUrl, String title, String description, java.util.List<String> tags) {
        String designId = java.util.UUID.randomUUID().toString();
        String tailorId = auth.getCurrentUser().getUid();

        Design design = new Design(designId, imageUrl, title, tailorId, description, tags);

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
                    resetUploadState("Failed to save data: " + e.getMessage());
                });
    }

    private void resetUploadState(String message) {
        Log.e(TAG, message);
        setUploadInProgress(false);
        if (getContext() != null) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Upload failed")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void setUploadInProgress(boolean inProgress) {
        btnUpload.setEnabled(!inProgress);
        btnUpload.setText(inProgress ? "Uploading..." : "Upload Design");
        if (progressBar != null) {
            progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        }
    }

    private byte[] readImageBytes(Uri uri) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Image file could not be opened");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_IMAGE_BYTES) {
                    throw new IOException("Image must be 5 MB or smaller");
                }
                output.write(buffer, 0, bytesRead);
            }
        }
        return output.toByteArray();
    }

    private String getFileExtension(Uri uri) {
        String mimeType = requireContext().getContentResolver().getType(uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return extension != null ? "." + extension : ".jpg";
    }

    private String formatStorageError(Exception e) {
        if (e instanceof StorageException) {
            StorageException storageException = (StorageException) e;
            return storageException.getMessage()
                    + " (code=" + storageException.getErrorCode()
                    + ", http=" + storageException.getHttpResultCode() + ")";
        }
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }

    private boolean isObjectNotFound(Exception e) {
        if (!(e instanceof StorageException)) {
            return false;
        }

        StorageException storageException = (StorageException) e;
        return storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND
                || storageException.getHttpResultCode() == 404;
    }

}
