package com.comp380.keeppace;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class AuthHelper {
    public static final int RC_SIGN_IN = 123;

    private static final String TAG = "AuthHelper";
    private static CallbackManager mCallbackManager;

    private static AuthCredential pendingFacebookCredential;

    public static void facebookSignIn(Activity activity, LoginButton loginButton) {

        mCallbackManager = CallbackManager.Factory.create();

        loginButton.setPermissions(Arrays.asList("email" ,"public_profile"));
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(activity, loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(@NonNull FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });
    }
    public static void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mCallbackManager != null)
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public static void handleFacebookAccessToken(Activity activity, AccessToken token) {

        AuthCredential facebookCredential =
                FacebookAuthProvider.getCredential(token.getToken());

        FirebaseAuth.getInstance()
                .signInWithCredential(facebookCredential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        // Facebook-only account → done
                        Log.d(TAG, "Facebook sign-in success");
                        goToHome(activity);
                        return; // navigation handled by Activity auth gate
                    }

                    if (task.getException() instanceof
                            com.google.firebase.auth.FirebaseAuthUserCollisionException) {

                        // 🔑 SAVE Facebook credential for later linking
                        pendingFacebookCredential = facebookCredential;

                        Log.d(TAG, "Collision detected. Need Google sign-in to link.");

                        Toast.makeText(activity,
                                "This account already exists with Google. Please sign in once to link Facebook.",
                                Toast.LENGTH_LONG).show();

                        googleSignIn(activity);

                    } else {
                        Log.e(TAG, "Facebook auth failed", task.getException());
                    }
                });
    }




    public static void goToHome (Activity activity) {
        Intent intent = new Intent(activity, HomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static Intent googleSignIn(Activity activity){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build();

        activity.startActivityForResult(intent, RC_SIGN_IN);
        return intent;
    }

    public static void signInHelper(Activity activity,
                                    int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {

        if (requestCode != RC_SIGN_IN) return;

        if (resultCode == Activity.RESULT_OK) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.d("AUTH", "Google sign-in success: " + user.getEmail());

            if (user != null && pendingFacebookCredential != null) {

                user.linkWithCredential(pendingFacebookCredential)
                        .addOnCompleteListener(linkTask -> {

                            if (linkTask.isSuccessful()) {
                                Log.d("AUTH", "✅ Facebook linked successfully");
                            } else {
                                Log.e("AUTH", "❌ Facebook linking failed",
                                        linkTask.getException());
                            }

                            // 🔴 ALWAYS clear after use
                            pendingFacebookCredential = null;
                        });
            }
        }
    }

    public static void signOut(Activity activity) {
        AuthUI.getInstance()
                .signOut(activity)
                .addOnCompleteListener(task -> {
                    com.facebook.login.LoginManager.getInstance().logOut();

                    Log.d(TAG, "User signed out from Firebase, Google, and Facebook");

                    // Wait for Firebase to fully clear the session
                    FirebaseAuth.getInstance().addAuthStateListener(auth -> {
                        if (auth.getCurrentUser() == null) {
                            Intent intent = new Intent(activity, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    });
                });
    }

}
