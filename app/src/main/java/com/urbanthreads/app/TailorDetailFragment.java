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

    private String userRole = "customer";

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                tailor = getArguments().getSerializable("tailor", models.User.class);
            } else {
                tailor = (models.User) getArguments().getSerializable("tailor");
            }
            tailorId = getArguments().getString("tailorId");
        }
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
            
            checkUserStatus();
        }

        view.findViewById(R.id.btnMessage).setOnClickListener(v -> {
            if (tailor != null && tailor.getPhoneNumber() != null && !tailor.getPhoneNumber().isEmpty()) {
                openWhatsApp(tailor.getPhoneNumber());
            } else {
                Toast.makeText(getContext(), "Tailor hasn't provided a contact number", Toast.LENGTH_SHORT).show();
            }
        });

        btnFollow.setOnClickListener(v -> toggleFollow());
        btnHire.setOnClickListener(v -> {
            if ("tailor".equals(userRole)) {
                // Navigate back to Home (where they see their designs)
                NavHostFragment.findNavController(this).popBackStack(R.id.nav_home, false);
            } else {
                toggleFollow();
            }
        });

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });

        return view;
    }

    private void checkUserStatus() {
        if (auth.getCurrentUser() == null) {
            btnFollow.setVisibility(View.GONE);
            btnHire.setText("Follow Tailor");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    models.User user = documentSnapshot.toObject(models.User.class);
                    if (user != null) {
                        userRole = user.getRole();
                        if (user.getFollowing() != null) {
                            isFollowing = user.getFollowing().contains(tailorId);
                        }

                        if ("tailor".equals(userRole)) {
                            // Tailor viewing a profile
                            btnFollow.setVisibility(View.GONE);
                            btnHire.setText("View My Designs");
                        } else {
                            // Customer viewing a profile
                            btnFollow.setVisibility(View.GONE); // Use main button for follow
                            updateFollowButton();
                        }
                    }
                });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnHire.setText("Following");
            btnHire.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            btnHire.setEnabled(false);
        } else {
            btnHire.setText("Follow Tailor");
            btnHire.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.purple_500)));
            btnHire.setEnabled(true);
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
}
