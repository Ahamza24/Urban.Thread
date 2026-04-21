package com.urbanthreads.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.FrameLayout;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private TextView tvName, tvEmail, tvRole, tvLocation;
    private Button btnLogout, btnAddWork, btnEditProfile;
    private RecyclerView rvPortfolio;
    private PortfolioAdapter adapter;
    private List<String> portfolioImages = new ArrayList<>();
    private models.User currentUser;
    private ImageView ivProfile;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImageToStorage(uri);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Explicitly using the bucket name to avoid 404 errors
        storage = FirebaseStorage.getInstance("gs://urban-threads-13a11.firebasestorage.app");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // We'll decide which layout to inflate based on the role, but initially we need to load data
        // Since onCreateView must return a view immediately, we'll return a placeholder or wait for data.
        // For simplicity, let's inflate a basic layout and then update it.
        return inflater.inflate(R.layout.fragment_profile_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }
        loadUserProfile(view);
    }

    private void loadUserProfile(View rootView) {
        if (auth.getCurrentUser() == null) return;
        
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        if (documentSnapshot.exists()) {
                            currentUser = documentSnapshot.toObject(models.User.class);
                            if (currentUser != null) {
                                setupUI(rootView, currentUser);
                            }
                        } else {
                            // Handle missing document (e.g., first-time login)
                            View progressBar = rootView.findViewById(R.id.progressBar);
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Profile not found. Please complete registration.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        View progressBar = rootView.findViewById(R.id.progressBar);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupUI(View rootView, models.User user) {
        FrameLayout container = rootView.findViewById(R.id.profile_container);
        View progressBar = rootView.findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        container.removeAllViews();
        
        int layoutId = "tailor".equals(user.getRole()) ? R.layout.activity_tailor_profile : R.layout.activity_customer_profile;
        View view = getLayoutInflater().inflate(layoutId, container, true);

        tvName = view.findViewById(R.id.tvName);
        tvRole = view.findViewById(R.id.tvRole);
        btnLogout = view.findViewById(R.id.btnLogout);
        ivProfile = view.findViewById(R.id.ivProfileImage);

        tvName.setText(user.getName());
        tvRole.setText("Role: " + user.getRole());

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(this).load(user.getProfileImage()).circleCrop().into(ivProfile);
        }

        if ("tailor".equals(user.getRole())) {
            tvLocation = view.findViewById(R.id.tvLocation);
            btnAddWork = view.findViewById(R.id.btnAddWork);
            btnEditProfile = view.findViewById(R.id.btnEditProfile);
            rvPortfolio = view.findViewById(R.id.rvPortfolio);

            String displayLocation = user.getShopName() != null ? user.getShopName() + " - " + user.getLocation() : user.getLocation();
            if (displayLocation != null) tvLocation.setText(displayLocation);

            portfolioImages = user.getPortfolio() != null ? user.getPortfolio() : new ArrayList<>();
            adapter = new PortfolioAdapter(portfolioImages);
            adapter.setOnItemLongClickListener((imageUrl, position) -> showDeleteConfirmation(imageUrl, position));
            rvPortfolio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvPortfolio.setAdapter(adapter);

            btnAddWork.setOnClickListener(v -> openImagePicker());
            btnEditProfile.setOnClickListener(v -> showEditDialog());
        } else {
            tvEmail = view.findViewById(R.id.tvEmail);
            if (tvEmail != null) tvEmail.setText(user.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });
    }

    private void showEditDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_tailor, null);
        builder.setView(view);

        EditText etShop = view.findViewById(R.id.editShopName);
        EditText etLoc = view.findViewById(R.id.editLocation);
        EditText etPh = view.findViewById(R.id.editPhone);
        EditText etDesc = view.findViewById(R.id.editDescription);
        Button btnSave = view.findViewById(R.id.btnSaveDetails);

        etShop.setText(currentUser.getShopName());
        etLoc.setText(currentUser.getLocation());
        etPh.setText(currentUser.getPhoneNumber());
        etDesc.setText(currentUser.getBusinessDescription());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) return;
            
            String shop = etShop.getText().toString().trim();
            String loc = etLoc.getText().toString().trim();
            String ph = etPh.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (shop.isEmpty() || loc.isEmpty() || ph.isEmpty() || desc.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(auth.getCurrentUser().getUid())
                    .update("shopName", shop, "location", loc, "phoneNumber", ph, "businessDescription", desc)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        if (getView() != null && getView().getParent() != null) {
                            loadUserProfile((View) getView().getParent()); // Refresh
                        }
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    private void openImagePicker() {
        mGetContent.launch("image/*");
    }

    private void uploadImageToStorage(Uri imageUri) {
        String fileName = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child("portfolio/" + fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateFirestorePortfolio(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFirestorePortfolio(String imageUrl) {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .update("portfolio", FieldValue.arrayUnion(imageUrl))
                .addOnSuccessListener(unused -> {
                    portfolioImages.add(imageUrl);
                    if (adapter != null) adapter.notifyItemInserted(portfolioImages.size() - 1);
                    Toast.makeText(getContext(), "Work added to portfolio!", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation(String imageUrl, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to remove this from your portfolio?")
                .setPositiveButton("Delete", (dialog, which) -> deletePortfolioItem(imageUrl, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePortfolioItem(String imageUrl, int position) {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .update("portfolio", FieldValue.arrayRemove(imageUrl))
                .addOnSuccessListener(aVoid -> {
                    portfolioImages.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                    
                    // Optional: Delete from Storage as well
                    try {
                        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete();
                    } catch (Exception e) {
                        // Ignore storage errors if URL is invalid
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
    }
}
