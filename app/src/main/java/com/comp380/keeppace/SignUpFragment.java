package com.comp380.keeppace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class SignUpFragment extends Fragment {
    private static final int RC_SIGN_IN = 123;

    private static final String TAG = "CreateUser";

    private FirebaseAuth mAuth;

    private FirebaseFirestore db;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        EditText fieldEmail = view.findViewById(R.id.fieldEmail);
        EditText fieldPassword = view.findViewById(R.id.fieldPassword);


        Button emailCreateAccountButton = view.findViewById(R.id.emailCreateAccountButton);
        emailCreateAccountButton.setOnClickListener(v -> {
            String email = fieldEmail.getText().toString().trim();
            String password = fieldPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(getActivity(), "Please fill in Email and Password", Toast.LENGTH_SHORT).show();
                return;
            }
            createAccount(email, password);
        });

        ImageView btn = view.findViewById(R.id.googleLogo);

        if (btn == null) {
            // 3) If this shows, the layout you set does NOT contain that ID
            Toast.makeText(getActivity(), "googleSignInButton NOT FOUND in layout", Toast.LENGTH_LONG).show();
            Log.e("Diag", "googleSignInButton not found. Are you using the right layout?");
            return view;
        } else {
            Toast.makeText(getActivity(), "Button found. Attaching listener…", Toast.LENGTH_SHORT).show();
            Log.d("Diag", "Button found. Attaching listener.");
        }

        btn.setOnClickListener(v -> {
            Intent intent = AuthHelper.googleSignIn(requireActivity());
            signInLauncher.launch(intent);
        });

        LoginButton fbBtn = view.findViewById(R.id.fbLoginButton);

        AuthHelper.facebookSignIn(requireActivity(), fbBtn);

        return view;
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            createUserDocumentIfNeeded(currentUser);
            reload();
        }
    }


    // [END on_start_check_user]
    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");

                        // Make sure Firestore has a user doc for this account
                        UserFirestoreHelper.ensureUserDocumentExists();

                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);

                        if (user != null) {
                            sendEmailVerification();
                            Intent intent = new Intent(getContext(), HomePage.class);
                            startActivity(intent);
                            getActivity().finish();
                        }

                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "User creation failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    // [END create_user_with_email]

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
    private void sendEmailVerification() {
        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        if(user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(getActivity(), task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("Auth", "sendEmailVerification failed", task.getException());
                            Toast.makeText(getContext(),
                                    "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    private void reload(){

    }
    private void updateUI(FirebaseUser user) {
        if(user != null){
            Toast.makeText(getContext(), "Welcome" + user.getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createUserDocumentIfNeeded(FirebaseUser firebaseUser) {

        Log.d(TAG, "createUserDocumentIfNeeded called");
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // Document doesn't exist yet: create it with initial data
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", email != null ? email : "Unknown"); // later you can use a username field
                        userData.put("email", email);
                        userData.put("score", 0); // starting value for leaderboard
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        userDocRef.set(userData)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "User document created for uid: " + uid)
                                )
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Failed to create user document", e)
                                );
                    } else {
                        Log.d(TAG, "User document already exists for uid: " + uid);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to check user document", e)
                );
    }

}