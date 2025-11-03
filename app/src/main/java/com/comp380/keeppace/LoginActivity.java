package com.comp380.keeppace;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Diag", "onCreate() started");                     // 1) prove Activity is alive
        setContentView(R.layout.activity_create_user);
        Log.d("Diag", "setContentView done");                    // 2) prove we loaded THIS layout

        Button btn = findViewById(R.id.googleSignInButton);

        if (btn == null) {
            // 3) If this shows, the layout you set does NOT contain that ID
            Toast.makeText(this, "googleSignInButton NOT FOUND in layout", Toast.LENGTH_LONG).show();
            Log.e("Diag", "googleSignInButton not found. Are you using the right layout?");
            return;
        } else {
            Toast.makeText(this, "Button found. Attaching listener…", Toast.LENGTH_SHORT).show();
            Log.d("Diag", "Button found. Attaching listener.");
        }

        btn.setOnClickListener(v -> {
            Toast.makeText(this, "Google button clicked!", Toast.LENGTH_SHORT).show();
            Log.d("Diag", "Click fired. Starting sign-in…");
            startSignIn();  // your existing method
        });
    }


    private void startSignIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d("Login", "Signed in as " + (user != null ? user.getEmail() : "null"));
                // TODO: navigate to your next screen
            } else {
                if (response != null && response.getError() != null) {
                    Log.w("Login", "Sign-in error", response.getError());
                } else {
                    Log.w("Login", "Sign-in cancelled");
                }
            }
        }
    }
}
