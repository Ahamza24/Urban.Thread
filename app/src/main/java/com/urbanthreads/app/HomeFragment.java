package com.urbanthreads.app;

import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView, recyclerTailors;
    private TextView tvNoDesigns, tvNoTailors;
    private android.widget.ProgressBar progressBar;
    private DesignAdapter adapter;
    private TailorAdapter tailorAdapter;
    private List<Design> designList;
    private List<models.User> tailorList;

    private FirebaseFirestore db;

    private EditText searchBar;
    private FloatingActionButton fabUpload;
    private com.google.android.material.chip.ChipGroup chipGroupFilter;
    private List<models.User> allTailors = new ArrayList<>();
    private List<String> followingIds = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // UI Connections
        recyclerView = view.findViewById(R.id.recyclerFeed);
        recyclerTailors = view.findViewById(R.id.recyclerTailors);
        tvNoDesigns = view.findViewById(R.id.tvNoDesigns);
        tvNoTailors = view.findViewById(R.id.tvNoTailors);
        progressBar = view.findViewById(R.id.progressBar);
        searchBar = view.findViewById(R.id.searchBar);
        fabUpload = view.findViewById(R.id.fabUpload);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        models.User user = doc.toObject(models.User.class);
                        if (user != null) {
                            if (user.getFollowing() != null) {
                                followingIds = user.getFollowing();
                            }
                            
                            // Only show fabUpload for tailors
                            if ("tailor".equals(user.getRole())) {
                                fabUpload.setVisibility(View.VISIBLE);
                            } else {
                                fabUpload.setVisibility(View.GONE);
                            }
                        }
                    });
        }

        // RecyclerView Setup - Designs
        designList = new ArrayList<>();
        adapter = new DesignAdapter(designList, design -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("design", design);
            try {
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_homeFragment_to_designDetailFragment, bundle);
            } catch (Exception e) {
                Log.e("HomeFragment", "Navigation failed", e);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // RecyclerView Setup - Tailors
        tailorList = new ArrayList<>();
        tailorAdapter = new TailorAdapter(tailorList);
        recyclerTailors.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerTailors.setAdapter(tailorAdapter);

        // Load Data
        loadDesigns();
        loadTailors();

        // Search
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                filterDesigns(query);
                filterTailors(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Upload Button
        fabUpload.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_homeFragment_to_uploadFragment);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Navigation target not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Filter Chip Logic
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String query = searchBar.getText() != null
                    ? searchBar.getText().toString().toLowerCase()
                    : "";
            filterTailors(query);
        });

        return view;
    }

    // Load Firestore Data - Tailors
    private void loadTailors() {
        db.collection("users")
                .whereEqualTo("role", "tailor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTailors.clear();
                    // We need to store the IDs for the "Following" filter to work
                    // This is best done inside the adapter or by wrapping the User object.
                    tailorAdapter.updateListWithIds(queryDocumentSnapshots);
                    
                    // Capture all tailors for filtering
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        models.User user = doc.toObject(models.User.class);
                        user.setId(doc.getId()); // ✅ SET ID HERE
                        allTailors.add(user);
                    }
                    
                    filterTailors(searchBar.getText().toString().toLowerCase());
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Tailors load failed", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load tailors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvNoTailors.setText("Error loading tailors");
                        tvNoTailors.setVisibility(View.VISIBLE);
                    }
                });
    }

    // Load Firestore Data - Designs
    private void loadDesigns() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        // Try to load with createdAt ordering first
        db.collection("designs")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (snapshots.isEmpty()) {
                        Log.w("HomeFragment", "Ordered query returned 0 results. Trying unordered fallback...");
                        loadDesignsUnordered();
                    } else {
                        processDesignResults(snapshots);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("HomeFragment", "Ordered load failed (possibly missing index), trying unordered fallback", e);
                    loadDesignsUnordered();
                });
    }

    private void loadDesignsUnordered() {
        db.collection("designs").get()
                .addOnSuccessListener(snapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    processDesignResults(snapshots);
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e("HomeFragment", "Designs load failed completely", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load designs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvNoDesigns.setText("Error: " + e.getLocalizedMessage());
                        tvNoDesigns.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void processDesignResults(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        designList.clear();
        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
            Design design = doc.toObject(Design.class);
            design.setId(doc.getId());
            designList.add(design);
        }

        // If ordered fetch succeeded but returned 0 results, it might be because existing docs lack 'timestamp'
        // However, we don't know here if it was the ordered or unordered fetch that succeeded.
        // For now, just update the UI.
        
        adapter.notifyDataSetChanged();
        tvNoDesigns.setVisibility(designList.isEmpty() ? View.VISIBLE : View.GONE);
        Log.d("HomeFragment", "Designs loaded: " + designList.size());
    }

    // Filter Designs
    private void filterDesigns(String query) {
        List<Design> filteredList = new ArrayList<>();

        for (Design d : designList) {
            boolean matchesTitle = d.getTitle() != null && d.getTitle().toLowerCase().contains(query);
            boolean matchesDescription = d.getDescription() != null && d.getDescription().toLowerCase().contains(query);
            
            boolean matchesTags = false;
            if (d.getTags() != null) {
                for (String tag : d.getTags()) {
                    if (tag.toLowerCase().contains(query)) {
                        matchesTags = true;
                        break;
                    }
                }
            }

            if (matchesTitle || matchesDescription || matchesTags) {
                filteredList.add(d);
            }
        }

        adapter.updateList(filteredList);
        tvNoDesigns.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Filter Tailors
    private void filterTailors(String query) {
        boolean showFollowingOnly = chipGroupFilter.getCheckedChipId() == R.id.chipFollowing;
        List<models.User> filteredList = new ArrayList<>();

        for (models.User t : allTailors) {
            boolean matchesSearch =
                    (t.getName() != null && t.getName().toLowerCase().contains(query)) ||
                            (t.getShopName() != null && t.getShopName().toLowerCase().contains(query)) ||
                            (t.getLocation() != null && t.getLocation().toLowerCase().contains(query));

            if (matchesSearch) {
                if (showFollowingOnly) {
                    if (t.getId() != null && followingIds.contains(t.getId())) {
                        filteredList.add(t);
                    }
                } else {
                    filteredList.add(t);
                }
            }
        }

        tailorAdapter.updateList(filteredList);
        tvNoTailors.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
