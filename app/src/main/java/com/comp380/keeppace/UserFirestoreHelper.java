package com.comp380.keeppace;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
/**
 * Helper class for creating and maintaining user documents in Firestore.
 *
 * <p>This utility makes sure that a Firestore user document exists for the currently
 * authenticated Firebase user. If no document, then create one with
 * default fields such as display name, email, score, and creation timestamp.
 *
 * <p>reason: intended to be called after a successful authentication event
 * to guarantee that backend user data is initialized, and to help us confirm.
 */
public class UserFirestoreHelper {

    private static final String TAG = "UserFirestoreHelper";

    public static void ensureUserDocumentExists() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "No current user, cannot ensure user document.");
            return;
        }

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // Create the document with initial data
                        Map<String, Object> userData = new HashMap<>();
                        String displayName = firebaseUser.getDisplayName();
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = (email != null ? email : "Unknown");
                        }

                        userData.put("displayName", displayName);
                        userData.put("email", email);
                        userData.put("score", 0);
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        userDocRef.set(userData)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "User document created for uid: " + uid))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Failed to create user document", e));
                    } else {
                        Log.d(TAG, "User document already exists for uid: " + uid);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to check user document", e)
                );
    }
}
