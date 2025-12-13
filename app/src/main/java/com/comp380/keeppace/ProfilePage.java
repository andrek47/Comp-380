package com.comp380.keeppace;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfilePage extends Fragment {

    private static final String TAG = "ProfilePage";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private TextView profileName, profileEmail;
    private TextView txtTotalScore, txtTotalDistance;
    private RecyclerView recyclerHistory;
    private RunHistoryAdapter adapter;
    private List<RunModel> runList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind UI
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        txtTotalScore = view.findViewById(R.id.txtTotalScore);
        txtTotalDistance = view.findViewById(R.id.txtTotalDistance);
        recyclerHistory = view.findViewById(R.id.recyclerHistory);

        // Setup RecyclerView
        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        runList = new ArrayList<>();
        adapter = new RunHistoryAdapter(runList);
        recyclerHistory.setAdapter(adapter);

        // NOTE: We removed loadProfileData() from here!
        // It is now in onResume() below.

        return view;
    }

    // --- THIS IS THE FIX ---
    @Override
    public void onResume() {
        super.onResume();
        // This runs every time you tap the Profile tab
        loadProfileData();
    }

    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            profileName.setText("Guest");
            return;
        }

        String uid = user.getUid();

        // 1. Load User Info
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String email = document.getString("email");
                        String name = document.getString("displayName");
                        Long score = document.getLong("score");

                        if (name == null || name.isEmpty()) {
                            name = (email != null) ? email.split("@")[0] : "User";
                        }
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }

                        profileName.setText(name);
                        profileEmail.setText(email);
                        txtTotalScore.setText(score != null ? String.valueOf(score) : "0");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user profile", e));

        // 2. Load Run History with SAFETY CHECKS
        db.collection("users").document(uid).collection("runs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    runList.clear();
                    double totalDistMeters = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            // Attempt to convert to our Java Object
                            RunModel run = doc.toObject(RunModel.class);

                            // CHECK 1: Did conversion fail completely?
                            if (run == null) continue;

                            // CHECK 2: Is the Timestamp missing? (Common in old data)
                            // If we don't check this, the App will crash when sorting dates
                            if (run.getTimestamp() == null) {
                                Log.w(TAG, "Skipping run with missing timestamp: " + doc.getId());
                                continue;
                            }

                            // If we got here, the data is safe to use!
                            runList.add(run);
                            totalDistMeters += run.getDistanceMeters();

                        } catch (Exception e) {
                            // If ANY error happens with this specific document,
                            // just Log it and keep going to the next one.
                            Log.w(TAG, "Skipping corrupted document: " + doc.getId());
                        }
                    }

                    // Update the list UI
                    adapter.notifyDataSetChanged();

                    // Update the Total Distance UI
                    // (Even if 0 runs were found, this will display 0.0)
                    double totalMiles = totalDistMeters * 0.000621371;

                    txtTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", totalMiles));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading runs", e);
                    txtTotalDistance.setText("Err");
                });
    }
}