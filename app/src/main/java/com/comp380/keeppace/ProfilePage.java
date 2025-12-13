package com.comp380.keeppace;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfilePage extends Fragment {

    private static final String TAG = "ProfilePage";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private ImageView imgProfileAvatar;
    private TextView profileName, profileEmail;
    private TextView txtTotalScore, txtTotalDistance;
    private RecyclerView recyclerHistory;
    private RunHistoryAdapter adapter;
    private List<RunModel> runList;

    // Avatar Options
    private final int[] AVATAR_RESOURCES = {
            R.drawable.ic_launcher_foreground,
            android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_mylocation,
            android.R.drawable.ic_media_play,
            android.R.drawable.star_big_on
    };

    private final String[] AVATAR_NAMES = {
            "Default Droid", "Camera Style", "Compass Style",
            "Location Style", "Runner Style", "Star Style"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind UI
        imgProfileAvatar = view.findViewById(R.id.imgProfileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        txtTotalScore = view.findViewById(R.id.txtTotalScore);
        txtTotalDistance = view.findViewById(R.id.txtTotalDistance);
        recyclerHistory = view.findViewById(R.id.recyclerHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        runList = new ArrayList<>();
        adapter = new RunHistoryAdapter(runList);
        recyclerHistory.setAdapter(adapter);

        imgProfileAvatar.setOnClickListener(v -> showAvatarSelectionDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void showAvatarSelectionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose Avatar")
                .setItems(AVATAR_NAMES, (dialog, which) -> saveAvatarSelection(which))
                .show();
    }

    private void saveAvatarSelection(int avatarIndex) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            imgProfileAvatar.setImageResource(AVATAR_RESOURCES[avatarIndex]);
            db.collection("users").document(user.getUid())
                    .update("avatarId", avatarIndex)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Avatar Updated!", Toast.LENGTH_SHORT).show());
        }
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
                        Long avatarIdLong = document.getLong("avatarId");
                        int avatarId = (avatarIdLong != null) ? avatarIdLong.intValue() : 0;

                        if (avatarId >= 0 && avatarId < AVATAR_RESOURCES.length) {
                            imgProfileAvatar.setImageResource(AVATAR_RESOURCES[avatarId]);
                        }

                        if (name == null || name.isEmpty()) {
                            name = (email != null) ? email.split("@")[0] : "User";
                        }
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }

                        // We set the name temporarily, but updateStreakUI will overwrite it
                        // with the fire emoji if a streak exists.
                        profileName.setText(name);
                        profileEmail.setText(email);
                        txtTotalScore.setText(score != null ? String.valueOf(score) : "0");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user profile", e));

        // 2. Load Run History
        db.collection("users").document(uid).collection("runs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    runList.clear();
                    double totalDistMeters = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            RunModel run = doc.toObject(RunModel.class);
                            if (run == null || run.getTimestamp() == null) continue;
                            runList.add(run);
                            totalDistMeters += run.getDistanceMeters();
                        } catch (Exception ignored) { }
                    }

                    adapter.notifyDataSetChanged();

                    // Calculate Total Miles
                    double totalMiles = totalDistMeters * 0.000621371;
                    txtTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", totalMiles));

                    // --- NEW: Calculate and Show Streak ---
                    updateStreakUI(runList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading runs", e));
    }

    // --- NEW STREAK LOGIC ---
    private void updateStreakUI(List<RunModel> runs) {
        int streak = calculateCurrentStreak(runs);

        // Get the current name displayed
        String currentNameText = profileName.getText().toString();

        if (currentNameText.contains(" \uD83D\uDD25")) {
            currentNameText = currentNameText.substring(0, currentNameText.indexOf(" \uD83D\uDD25"));
        }

        if (streak > 1) {
            // Append Fire Emoji AND the streak number
            profileName.setText(currentNameText + " \uD83D\uDD25 " + streak);
        } else {
            // No streak (or just 1 day), just show name
            profileName.setText(currentNameText);
        }
    }

    private int calculateCurrentStreak(List<RunModel> runs) {
        if (runs.isEmpty()) return 0;

        // 1. Get Unique Run Dates (yyyy-MM-dd)
        Set<String> uniqueRunDates = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (RunModel run : runs) {
            if (run.getTimestamp() != null) {
                uniqueRunDates.add(sdf.format(run.getTimestamp().toDate()));
            }
        }

        // 2. Check Backwards from Today
        int streakCount = 0;
        Calendar calendar = Calendar.getInstance(); // Starts at Today

        // Check Today
        String today = sdf.format(calendar.getTime());
        if (uniqueRunDates.contains(today)) {
            streakCount++;
        }

        // Loop backwards
        while (true) {
            // Move back 1 day
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            String previousDay = sdf.format(calendar.getTime());

            if (uniqueRunDates.contains(previousDay)) {
                streakCount++;
            } else {
                // Streak Broken!
                // Exception: If I haven't run TODAY yet, but I ran YESTERDAY,
                // my streak is technically still active (pending today's run).
                // But typically a "Current Streak" number implies consecutive days completed.
                // Logic: If streakCount is 0 (didn't run today) but ran yesterday, count continues.

                // If we are at the very first step (Checking Yesterday) and streak is 0 (No run today)
                if (streakCount == 0 && uniqueRunDates.contains(previousDay)) {
                    // This handles the "I haven't run yet today" case
                    continue;
                }
                break;
            }
        }
        return streakCount;
    }
}