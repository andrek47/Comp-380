package com.comp380.keeppace;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class AuthHelper {
    public static final int RC_SIGN_IN = 123;

    public static void googleSignIn(Activity activity){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();

        activity.startActivityForResult(intent, RC_SIGN_IN);
    }

    public static void signInHelper (Activity activity, int requestCode, int resultCode, @Nullable Intent data){
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == Activity.RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d("Login", "Signed in as " + (user != null ? user.getEmail() : "null"));
                // TODO: navigate to your next screen
                Intent intent = new Intent(activity, HomePage.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finish();
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
