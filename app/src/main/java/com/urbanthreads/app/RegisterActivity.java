package com.urbanthreads.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etName, etEmail, etPassword, etShopName, etLocation, etPhone, etDescription;
    private TextInputLayout tilName, tilEmail, tilPassword, tilShopName, tilLocation, tilPhone, tilDescription;
    private RadioButton rbCustomer, rbTailor, rbMale, rbFemale;
    private LinearLayout tailorFields;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        rbCustomer = findViewById(R.id.rbCustomer);
        rbTailor = findViewById(R.id.rbTailor);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);

        tailorFields = findViewById(R.id.tailorFields);
        etShopName = findViewById(R.id.etShopName);
        etLocation = findViewById(R.id.etLocation);
        etPhone = findViewById(R.id.etPhone);
        etDescription = findViewById(R.id.etDescription);
        
        tilShopName = findViewById(R.id.tilShopName);
        tilLocation = findViewById(R.id.tilLocation);
        tilPhone = findViewById(R.id.tilPhone);
        tilDescription = findViewById(R.id.tilDescription);
        
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        rbTailor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tailorFields.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    private void registerUser() {
        // Reset errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (tilShopName != null) tilShopName.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = rbCustomer.isChecked() ? "customer" : "tailor";

        boolean hasError = false;

        if (name.isEmpty()) {
            tilName.setError("Full Name is required");
            hasError = true;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            hasError = true;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            hasError = true;
        }

        if ("tailor".equals(role)) {
            if (etShopName.getText().toString().trim().isEmpty()) {
                tilShopName.setError("Shop Name is required");
                hasError = true;
            }
        }

        if (hasError) return;

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();

                    // Create User object using the static inner class
                    models.User user = new models.User(name, email, role);
                    if ("tailor".equals(role)) {
                        user.setShopName(etShopName.getText().toString());
                        user.setLocation(etLocation.getText().toString());
                        user.setPhoneNumber(etPhone.getText().toString());
                        user.setBusinessDescription(etDescription.getText().toString());
                        String gender = rbMale.isChecked() ? "Male Clothing" : "Female Clothing";
                        user.setGender(gender);
                    }

                    db.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                // Navigate to MainActivity
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
