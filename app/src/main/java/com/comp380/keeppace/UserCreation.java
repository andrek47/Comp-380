package com.comp380.keeppace;

/*import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class UserCreation extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    private static final String TAG = "CreateUser";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_signup);

        mAuth = FirebaseAuth.getInstance();

        EditText fieldEmail = findViewById(R.id.fieldEmail);
        EditText fieldPassword = findViewById(R.id.fieldPassword);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

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

        ImageView btn = findViewById(R.id.googleLogo);

        if (btn == null) {
            // 3) If this shows, the layout you set does NOT contain that ID
            Toast.makeText(this, "googleSignInButton NOT FOUND in layout", Toast.LENGTH_LONG).show();
            Log.e("Diag", "googleSignInButton not found. Are you using the right layout?");
            return;
        } else {
            Toast.makeText(this, "Button found. Attaching listener…", Toast.LENGTH_SHORT).show();
            Log.d("Diag", "Button found. Attaching listener.");
        }

        btn.setOnClickListener(v -> AuthHelper.googleSignIn(this));

        LoginButton fbBtn = findViewById(R.id.fbLoginButton);

        AuthHelper.facebookSignIn(this, fbBtn);
    }

    // [START on_start_check_user]
   @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            reload();
        }
    }

    // [END on_start_check_user]
    private void createAccount(String email, String password) {
        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(UserCreation.this, task -> {
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
                        // If sign up fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(UserCreation.this, "User creation failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });


    }
        // [END create_user_with_email]

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AuthHelper.onActivityResult(requestCode, resultCode, data);
        AuthHelper.signInHelper(this, requestCode, resultCode, data);
    }
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
    }
    private void reload(){

    }
    private void updateUI(FirebaseUser user) {
        if(user != null){
            Toast.makeText(this, "Welcome" + user.getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}*/