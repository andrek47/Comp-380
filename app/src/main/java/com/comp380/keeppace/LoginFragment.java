package com.comp380.keeppace;

import static com.comp380.keeppace.AuthHelper.RC_SIGN_IN;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]
    private FirebaseFirestore db;

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        // connects to the XML layout

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());

        // [START initialize_auth]
        // Initialize Firebase Auth
        FirebaseApp.initializeApp(requireContext());
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();



        TextInputEditText emailField = view.findViewById(R.id.fieldEmail);
        TextInputEditText passwordField = view.findViewById(R.id.fieldPassword);



        Button emailSignInButton = view.findViewById(R.id.emailSignInButton);
        emailSignInButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(requireActivity(), "Please fill in Email and Password", Toast.LENGTH_SHORT).show();
                return;
            }
            signIn(email, password);
        });

        ImageView btn = view.findViewById(R.id.googleLogo);

        btn.setOnClickListener(v -> {
            Intent intent = AuthHelper.googleSignIn(requireActivity());
            signInLauncher.launch(intent);
        });
        return view;
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Ensure Firestore user document exists for any already-logged-in user (including Google)
            createUserDocumentIfNeeded(currentUser);
            reload();
        }
    }

    // [END on_start_check_user]



    private void signIn(String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");

                            // Ensure user doc exists in Firestore
                            UserFirestoreHelper.ensureUserDocumentExists();

                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.METHOD, "button_click");
                            bundle.putString(FirebaseAnalytics.Param.METHOD, "button_click");
                            firebaseAnalytics.logEvent("test_firebase_event", bundle);
                            Toast.makeText(requireActivity(), "Firebase event logged!", Toast.LENGTH_SHORT).show();

                            if(user != null) {
                                Intent intent = new Intent(requireActivity(), HomePage.class);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(requireActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                            }
                        }
                    });
        // [END sign_in_with_email]
    }

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        int resultCode = result.getResultCode();
                        Intent data = result.getData();

                        // Handle the result here
                        AuthHelper.onActivityResult(RC_SIGN_IN, resultCode, data);
                        AuthHelper.signInHelper(requireActivity(), RC_SIGN_IN, resultCode, data);
                    });

    private void reload() { }

    private void updateUI(FirebaseUser user) {

    }
    private void createUserDocumentIfNeeded(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // Document doesn't exist yet: create it with initial data
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", email != null ? email : "Unknown"); // or a username later
                        userData.put("email", email);
                        userData.put("score", 0); // starting stat for leaderboard
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        userDocRef.set(userData)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "User document created for uid: " + uid)
                                )
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Failed to create user document", e)
                                );
                    } else {
                        // Document already exists; nothing to do
                        Log.d(TAG, "User document already exists for uid: " + uid);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to check user document", e)
                );
    }




}
