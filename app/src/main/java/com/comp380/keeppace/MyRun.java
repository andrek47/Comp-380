package com.comp380.keeppace;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyRun extends Fragment {

    private static final String TAG = "MyRun";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Find the button in the layout
        Button incrementButton = view.findViewById(R.id.buttonIncrementScore);

        // Attach click listener
        incrementButton.setOnClickListener(v -> incrementUserScore());

        // (Optional) any other UI setup, e.g. TextView, etc.
        // TextView someText = view.findViewById(R.id.someText);

        return view;
    }

    private void incrementUserScore() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No user logged in. Cannot update score.");
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid)
                .update("score", FieldValue.increment(1))
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Score incremented by 1")
                )
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to increment score", e)
                );
    }
}
