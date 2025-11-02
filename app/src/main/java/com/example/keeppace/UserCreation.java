package com.example.keeppace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class UserCreation extends AppCompatActivity {

    private static final String TAG = "CreateUser";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        mAuth = FirebaseAuth.getInstance();

        EditText fieldEmail = findViewById(R.id.fieldEmail);
        EditText fieldPassword = findViewById(R.id.fieldPassword);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button emailCreateAccountButton = findViewById(R.id.emailCreateAccountButton);
        emailCreateAccountButton.setOnClickListener(view -> {
            String email = fieldEmail.getText().toString().trim();
            String password = fieldPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(UserCreation.this, "Please fill in Email and Password", Toast.LENGTH_SHORT).show();
                return;
            }
            createAccount(email, password);
        });
    }

    // [START on_start_check_user]
   /* @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            reload();
        }
    }*/

    // [END on_start_check_user]
    private void createAccount(String email, String password) {
        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(UserCreation.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            if (user != null) {
                                user.sendEmailVerification();
                                Intent intent = new Intent(UserCreation.this, HomePage.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(UserCreation.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });


    }
        // [END create_user_with_email]


    private void sendEmailVerification() {
        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Email sent
                    }
                });
        // [END send_email_verification]
    }

    private void reload() {
    }

    private void updateUI(FirebaseUser user) {
        if(user != null){
            Toast.makeText(this, "Welcome" + user.getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}