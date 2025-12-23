/**
 * SettingsFragment - user account settings.
 * shows app preferences, allows users to delete their account (which they should never do)
 * handles reauthentication if required, and redirects to the login screen.
 */

package com.comp380.keeppace.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.comp380.keeppace.R;
import com.comp380.keeppace.main.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends PreferenceFragmentCompat {


    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference deletePref = findPreference("delete_account");

        if (deletePref != null) {
            deletePref.setOnPreferenceClickListener(preference -> {
                showDeleteConfirmation();
                return true;
            });
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This action is permanent. Your account and all data will be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No authenticated user");
            return;
        }

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");

                            if (isAdded()) {
                                goToLogin();
                            }
                        }else{
                            handleDeleteError(task.getException());
                        }
                    }
                });
    }
    private void handleDeleteError(Exception e) {
        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            Toast.makeText(getContext(),
                    "Please sign in again to delete your account",
                    Toast.LENGTH_LONG).show();

            FirebaseAuth.getInstance().signOut();
            goToLogin();
        } else {
            Log.e(TAG, "Delete failed", e);
            Toast.makeText(getContext(),
                    "Failed to delete account",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void goToLogin() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}