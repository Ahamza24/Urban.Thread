package com.urbanthreads.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.bumptech.glide.Glide;

import java.io.Serializable;
import java.util.List;

public class TailorDetailFragment extends Fragment {

    private models.User tailor;
    private ImageView ivHeader;
    private TextView tvShopName, tvLocation, tvBio;
    private RecyclerView recyclerPortfolio;
    private PortfolioAdapter portfolioAdapter;
    private android.widget.Button btnFollow, btnHire;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String tailorId;
    private boolean isFollowing = false;

    public TailorDetailFragment() {
        // Required empty public constructor
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                tailor = getArguments().getSerializable("tailor", models.User.class);
            } else {
                tailor = (models.User) getArguments().getSerializable("tailor");
            }
            tailorId = getArguments().getString("tailorId");
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tailor_detail, container, false);

        ivHeader = view.findViewById(R.id.ivHeaderImage);
        tvShopName = view.findViewById(R.id.tvShopNameDetail);
        tvLocation = view.findViewById(R.id.tvLocationDetail);
        tvBio = view.findViewById(R.id.tvBioDetail);
        recyclerPortfolio = view.findViewById(R.id.recyclerPortfolio);
        btnFollow = view.findViewById(R.id.btnFollow);
        btnHire = view.findViewById(R.id.btnHireNowDetail);

        if (tailor != null) {
            // If tailorId wasn't passed, we might need it from the object or another way
            // For now assume it was passed or the object is enough for display.
            
            tvShopName.setText(tailor.getShopName() != null ? tailor.getShopName() : tailor.getName());
            tvLocation.setText(tailor.getLocation() != null ? tailor.getLocation() : "No location specified");
            tvBio.setText(tailor.getBusinessDescription() != null ? tailor.getBusinessDescription() : "No bio available");

            String imageUrl = null;
            if (tailor.getProfileImage() != null && !tailor.getProfileImage().isEmpty()) {
                imageUrl = tailor.getProfileImage();
            } else if (tailor.getPortfolio() != null && !tailor.getPortfolio().isEmpty()) {
                imageUrl = tailor.getPortfolio().get(0);
            }

            if (imageUrl != null) {
                Glide.with(this).load(imageUrl).into(ivHeader);
            }

            if (tailor.getPortfolio() != null) {
                portfolioAdapter = new PortfolioAdapter(tailor.getPortfolio());
                recyclerPortfolio.setLayoutManager(new GridLayoutManager(getContext(), 3));
                recyclerPortfolio.setAdapter(portfolioAdapter);
            }
            
            checkFollowStatus();
        }

        view.findViewById(R.id.btnMessage).setOnClickListener(v -> {
            if (tailor != null && tailor.getPhoneNumber() != null && !tailor.getPhoneNumber().isEmpty()) {
                openWhatsApp(tailor.getPhoneNumber());
            } else {
                Toast.makeText(getContext(), "Tailor hasn't provided a contact number", Toast.LENGTH_SHORT).show();
            }
        });

        btnFollow.setOnClickListener(v -> toggleFollow());
        btnHire.setOnClickListener(v -> handleHireAction());

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });

        return view;
    }

    private void checkFollowStatus() {
        if (auth.getCurrentUser() == null || tailorId == null) {
            btnFollow.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    models.User user = documentSnapshot.toObject(models.User.class);
                    if (user != null && user.getFollowing() != null) {
                        isFollowing = user.getFollowing().contains(tailorId);
                        updateFollowButton();
                    }
                });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
        } else {
            btnFollow.setText("Follow");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.purple_500)));
        }
    }

    private void toggleFollow() {
        if (auth.getCurrentUser() == null || tailorId == null) return;

        String userId = auth.getCurrentUser().getUid();
        if (isFollowing) {
            db.collection("users").document(userId)
                    .update("following", FieldValue.arrayRemove(tailorId))
                    .addOnSuccessListener(aVoid -> {
                        isFollowing = false;
                        updateFollowButton();
                    });
        } else {
            db.collection("users").document(userId)
                    .update("following", FieldValue.arrayUnion(tailorId))
                    .addOnSuccessListener(aVoid -> {
                        isFollowing = true;
                        updateFollowButton();
                    });
        }
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            // Remove any non-numeric characters from the phone number
            String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
            // WhatsApp requires country code, but we'll try to use what's provided
            String url = "https://wa.me/" + cleanNumber;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleHireAction() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to hire a tailor", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tailorId == null && tailor != null) {
            // Try to find the tailorId from the database if not passed
            // For now, let's assume it should have been passed or we can't proceed reliably
            Toast.makeText(getContext(), "Error: Tailor ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerId = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> order = new java.util.HashMap<>();
        order.put("customerId", customerId);
        order.put("tailorId", tailorId);
        order.put("status", "pending");
        order.put("type", "general_hire");
        order.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Hire request sent to " + (tailor != null ? tailor.getShopName() : "tailor"), Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
