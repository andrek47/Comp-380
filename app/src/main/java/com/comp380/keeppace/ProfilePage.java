package com.comp380.keeppace;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;



public class ProfilePage extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView textScoreValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);
        //make an username out of email
        TextView username = view.findViewById(R.id.username);


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        textScoreValue = view.findViewById(R.id.textScoreValue);

        loadUserScore();

        Button refreshButton = view.findViewById(R.id.buttonRefreshScore);
        refreshButton.setOnClickListener(v -> loadUserScore());


        //check if there is an user
        if (user != null) {
            String email = user.getEmail(); // get user email
            String name = email != null ? email.split("@")[0] : "Guest"; // split the email to only get till @

            username.setText(name.substring(0,1).toUpperCase() + name.substring(1)); //set the username
        } else {
            username.setText("Email: Not signed in"); //not logged in
        }

        return view;
    }

    private void loadUserScore() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            textScoreValue.setText("N/A");
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Long score = snapshot.getLong("score");
                        if (score == null) score = 0L;

                        textScoreValue.setText(String.valueOf(score));
                    } else {
                        textScoreValue.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    textScoreValue.setText("0");
                    Log.w("Profile", "Failed to load user score", e);
                });
    }

}