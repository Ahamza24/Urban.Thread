package com.urbanthreads.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class  DesignDetailFragment extends Fragment {

    private Design design;
    private models.User tailor;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ImageView imgDesign, imgTailor;
    private TextView tvTitle, tvCategory, tvTailorName;
    private ChipGroup cgTags;
    private Button btnOrder;

    private String userRole = "customer";
    private boolean isFollowing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                design = getArguments().getSerializable("design", Design.class);
            } else {
                design = (Design) getArguments().getSerializable("design");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_design_detail, container, false);

        imgDesign = view.findViewById(R.id.imgDetailDesign);
        imgTailor = view.findViewById(R.id.imgTailorProfile);
        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvCategory = view.findViewById(R.id.tvDetailCategory);
        tvTailorName = view.findViewById(R.id.tvTailorName);
        cgTags = view.findViewById(R.id.cgDetailTags);
        btnOrder = view.findViewById(R.id.btnOrderNow);

        if (design != null) {
            displayDesignDetails();
            fetchTailorInfo();
        }

        checkUserRoleAndStatus();

        btnOrder.setOnClickListener(v -> {
            if ("tailor".equals(userRole)) {
                // Navigate to My Designs (Home)
                NavHostFragment.findNavController(this).popBackStack(R.id.nav_home, false);
            } else {
                handleFollowAction();
            }
        });

        view.findViewById(R.id.layoutTailorInfo).setOnClickListener(v -> {
            if (tailor != null) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("tailor", tailor);
                bundle.putString("tailorId", design.getTailorId());
                NavHostFragment.findNavController(this).navigate(R.id.tailorDetailFragment, bundle);
            }
        });

        return view;
    }

    private void displayDesignDetails() {
        tvTitle.setText(design.getTitle());
        tvCategory.setText(design.getDescription());

        Glide.with(this)
                .load(design.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgDesign);

        if (design.getTags() != null) {
            for (String tag : design.getTags()) {
                Chip chip = new Chip(getContext());
                chip.setText(tag);
                cgTags.addView(chip);
            }
        }
    }

    private void fetchTailorInfo() {
        db.collection("users").document(design.getTailorId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    tailor = documentSnapshot.toObject(models.User.class);
                    if (tailor != null) {
                        tvTailorName.setText(tailor.getName() != null ? tailor.getName() : "Unknown Tailor");
                        if (tailor.getProfileImage() != null && !tailor.getProfileImage().isEmpty()) {
                            Glide.with(this).load(tailor.getProfileImage()).circleCrop().into(imgTailor);
                        }
                    }
                });
    }

    private void checkUserRoleAndStatus() {
        if (auth.getCurrentUser() == null) {
            btnOrder.setText("Follow Tailor");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userRole = doc.getString("role");
                        List<String> following = (List<String>) doc.get("following");
                        isFollowing = following != null && following.contains(design.getTailorId());

                        if ("tailor".equals(userRole)) {
                            btnOrder.setText("View My Designs");
                        } else {
                            updateFollowButton();
                        }
                    }
                });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnOrder.setText("Following");
            btnOrder.setEnabled(false);
            btnOrder.setAlpha(0.6f);
        } else {
            btnOrder.setText("Follow Tailor");
            btnOrder.setEnabled(true);
            btnOrder.setAlpha(1.0f);
        }
    }

    private void handleFollowAction() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to follow tailors", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String tailorId = design.getTailorId();

        db.collection("users").document(uid)
                .update("following", com.google.firebase.firestore.FieldValue.arrayUnion(tailorId))
                .addOnSuccessListener(aVoid -> {
                    isFollowing = true;
                    updateFollowButton();
                    Toast.makeText(getContext(), "Now following " + (tailor != null ? tailor.getName() : "tailor"), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
