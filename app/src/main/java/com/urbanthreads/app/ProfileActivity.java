package com.urbanthreads.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvName, tvEmail, tvRole, tvLocation;
    private Button btnLogout, btnEditProfile;

    private models.User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    currentUser = doc.toObject(models.User.class);
                    if (currentUser != null) {
                        setupUI(currentUser);
                    } else {
                        Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void setupUI(models.User user) {

        if ("tailor".equals(user.getRole())) {
            setContentView(R.layout.activity_tailor_profile);

            tvLocation = findViewById(R.id.tvLocation);
            btnEditProfile = findViewById(R.id.btnEditProfile);

            String shop = user.getShopName() != null ? user.getShopName() : "";
            String loc = user.getLocation() != null ? user.getLocation() : "";
            tvLocation.setText(shop.isEmpty() ? loc : shop + " - " + loc);

            btnEditProfile.setOnClickListener(v -> showEditDialog());

        } else {
            setContentView(R.layout.activity_customer_profile);

            tvEmail = findViewById(R.id.tvEmail);
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }

        tvName = findViewById(R.id.tvName);
        tvRole = findViewById(R.id.tvRole);
        btnLogout = findViewById(R.id.btnLogout);

        tvName.setText(user.getName() != null ? user.getName() : "User");
        tvRole.setText("Role: " + user.getRole());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showEditDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_tailor, null);
        builder.setView(view);

        EditText etShop = view.findViewById(R.id.editShopName);
        EditText etLoc = view.findViewById(R.id.editLocation);
        EditText etPh = view.findViewById(R.id.editPhone);
        EditText etDesc = view.findViewById(R.id.editDescription);
        Button btnSave = view.findViewById(R.id.btnSaveDetails);

        etShop.setText(currentUser.getShopName() != null ? currentUser.getShopName() : "");
        etLoc.setText(currentUser.getLocation() != null ? currentUser.getLocation() : "");
        etPh.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        etDesc.setText(currentUser.getBusinessDescription() != null ? currentUser.getBusinessDescription() : "");

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String shop = etShop.getText().toString().trim();
            String loc = etLoc.getText().toString().trim();
            String ph = etPh.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (shop.isEmpty() || loc.isEmpty()) {
                Toast.makeText(this, "Shop name and location required", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);

            db.collection("users").document(auth.getCurrentUser().getUid())
                    .update("shopName", shop,
                            "location", loc,
                            "phoneNumber", ph,
                            "businessDescription", desc)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUserProfile();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}