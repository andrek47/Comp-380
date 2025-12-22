/**
 * - podium section showing 1st, 2nd, and 3rd place
 * - table displaying the top 5 users from Firestore
 * - refresh button to reload leaderboard
 */
package com.comp380.keeppace.leaderboard;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import com.comp380.keeppace.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class Leaderboard extends Fragment {

    private static final String TAG = "Leaderboard";

    private FirebaseFirestore db;

    // Podium
    private TextView txtFirst;
    private TextView txtSecond;
    private TextView txtThird;

    // Table rows for top 5
    private TextView[] nameViews;
    private TextView[] scoreViews;
    private TextView[] rankViews;
    SwipeRefreshLayout swipeRefresh;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        swipeRefresh.setOnRefreshListener(() -> {
            loadLeaderboard();
            swipeRefresh.setRefreshing(false);
        });

        // Podium TextViews
        txtFirst = view.findViewById(R.id.txt_first);
        txtSecond = view.findViewById(R.id.txt_second);
        txtThird = view.findViewById(R.id.txt_third);

        // Table rows (we’ll use arrays to simplify updating)
        nameViews = new TextView[] {
                view.findViewById(R.id.name1),
                view.findViewById(R.id.name2),
                view.findViewById(R.id.name3),
                view.findViewById(R.id.name4),
                view.findViewById(R.id.name5)
        };

        scoreViews = new TextView[] {
                view.findViewById(R.id.score1),
                view.findViewById(R.id.score2),
                view.findViewById(R.id.score3),
                view.findViewById(R.id.score4),
                view.findViewById(R.id.score5)
        };

        rankViews = new TextView[] {
                view.findViewById(R.id.rank1),
                view.findViewById(R.id.rank2),
                view.findViewById(R.id.rank3),
                view.findViewById(R.id.rank4),
                view.findViewById(R.id.rank5)
        };

        Button refreshButton = view.findViewById(R.id.buttonRefreshLeaderboard);
        refreshButton.setOnClickListener(v -> loadLeaderboard());

        // Load leaderboard data from Firestore
        loadLeaderboard();

        return view;
    }

    private void loadLeaderboard() {
        db.collection("users")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();

                    // Clear everything first
                    clearLeaderboardViews();

                    for (int i = 0; i < docs.size() && i < 5; i++) {
                        DocumentSnapshot doc = docs.get(i);

                        String name = doc.getString("displayName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("email");
                        }
                        if (name == null) {
                            name = "Unknown";
                        }

                        Long scoreLong = doc.getLong("score");
                        long score = scoreLong != null ? scoreLong : 0;

                        // Rank is simply index + 1
                        int rank = i + 1;

                        // Update table row
                        nameViews[i].setText(name);
                        scoreViews[i].setText(String.valueOf(score));
                        rankViews[i].setText(String.valueOf(rank));

                        // Update podium text for top 3
                        if (rank == 1) {
                            txtFirst.setText(name);
                        } else if (rank == 2) {
                            txtSecond.setText(name);
                        } else if (rank == 3) {
                            txtThird.setText(name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to load leaderboard", e);
                });
    }

    private void clearLeaderboardViews() {
        // Clear podium
        txtFirst.setText("");
        txtSecond.setText("");
        txtThird.setText("");

        // Clear table rows
        for (int i = 0; i < nameViews.length; i++) {
            nameViews[i].setText("");
            scoreViews[i].setText("");
            rankViews[i].setText("");
        }

    }

}