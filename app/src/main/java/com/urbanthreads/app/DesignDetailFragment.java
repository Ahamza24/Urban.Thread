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

public class DesignDetailFragment extends Fragment {

    private Design design;
    private models.User tailor;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ImageView imgDesign, imgTailor;
    private TextView tvTitle, tvCategory, tvTailorName;
    private ChipGroup cgTags;
    private Button btnOrder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (design != null) {
            displayDesignDetails();
            fetchTailorInfo();
        }

        btnOrder.setOnClickListener(v -> handleHireAction());

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
        tvCategory.setText(design.getCategory());

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

    private void handleHireAction() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to hire a tailor", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic for hiring: For now, we'll just show a success toast or navigate to a chat/order screen
        // In a real app, this would create an 'order' or 'inquiry' record in Firestore
        String customerId = auth.getCurrentUser().getUid();
        String tailorId = design.getTailorId();

        java.util.Map<String, Object> order = new java.util.HashMap<>();
        order.put("designId", design.getId());
        order.put("customerId", customerId);
        order.put("tailorId", tailorId);
        order.put("status", "pending");
        order.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Hire request sent to " + (tailor != null ? tailor.getName() : "tailor"), Toast.LENGTH_LONG).show();
                    // Optionally navigate to an 'Orders' screen
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
