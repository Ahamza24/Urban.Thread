package com.urbanthreads.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.FrameLayout;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvName, tvEmail, tvRole, tvLocation;
    private TextView tvPostCount, tvFollowerCount, tvFollowingCount, tvOrderCount;
    private Button btnLogout, btnEditProfile;
    private models.User currentUser;
    private ImageView ivProfile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                                fetchStats(currentUser);
                            }
                        } else {
                            View progressBar = rootView.findViewById(R.id.progressBar);
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_LONG).show();
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
        tvRole.setText(user.getRole().toUpperCase());

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(this).load(user.getProfileImage()).circleCrop().into(ivProfile);
        }

        if ("tailor".equals(user.getRole())) {
            tvLocation = view.findViewById(R.id.tvLocation);
            btnEditProfile = view.findViewById(R.id.btnEditProfile);
            tvPostCount = view.findViewById(R.id.tvPostCount);
            tvFollowerCount = view.findViewById(R.id.tvFollowerCount);
            tvFollowingCount = view.findViewById(R.id.tvFollowingCount);

            String displayLocation = user.getShopName() != null ? user.getShopName() + " - " + user.getLocation() : user.getLocation();
            if (displayLocation != null) tvLocation.setText(displayLocation);

            btnEditProfile.setOnClickListener(v -> showEditDialog());
        } else {
            tvEmail = view.findViewById(R.id.tvEmail);
            tvOrderCount = view.findViewById(R.id.tvOrderCount);
            tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
            btnEditProfile = view.findViewById(R.id.btnEditProfile);
            
            if (tvEmail != null) tvEmail.setText(user.getEmail());
            btnEditProfile.setOnClickListener(v -> showEditDialog());
        }

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });
    }

    private void fetchStats(models.User user) {
        String userId = auth.getCurrentUser().getUid();

        // Following Count
        int followingCount = user.getFollowing() != null ? user.getFollowing().size() : 0;
        if (tvFollowingCount != null) tvFollowingCount.setText(String.valueOf(followingCount));

        if ("tailor".equals(user.getRole())) {
            // Design (Post) Count
            db.collection("designs")
                    .whereEqualTo("tailorId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvPostCount != null) tvPostCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    });

            // Follower Count (Users who follow this tailor)
            db.collection("users")
                    .whereArrayContains("following", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvFollowerCount != null) tvFollowerCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    });
        } else {
            // Order Count
            db.collection("orders")
                    .whereEqualTo("customerId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvOrderCount != null) tvOrderCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    });
        }
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
}
