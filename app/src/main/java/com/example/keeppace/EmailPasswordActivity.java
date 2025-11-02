/**
 * Copyright 2021 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.keeppace;

//import android.app.Activity;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailPasswordActivity extends AppCompatActivity {

    private static final String TAG = "EmailPassword";
    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_password); // connects to the XML layout

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // [START initialize_auth]
        // Initialize Firebase Auth
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        EditText emailField = findViewById(R.id.fieldEmail);
        EditText passwordField = findViewById(R.id.fieldPassword);

        Button emailSignInButton = findViewById(R.id.emailSignInButton);
        emailSignInButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            signIn(email, password);
        });
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            reload();
        }
    }
    // [END on_start_check_user]



    private void signIn(String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.METHOD, "button_click");
                            firebaseAnalytics.logEvent("test_firebase_event", bundle);
                            Toast.makeText(EmailPasswordActivity.this, "Firebase event logged!", Toast.LENGTH_SHORT).show();

                            if(user != null) {
                                Intent intent = new Intent(EmailPasswordActivity.this, HomePage.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private void reload() { }

    private void updateUI(FirebaseUser user) {

    }
}
