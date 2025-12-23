/**
 * MySettingsActivity does the app settings screen.
 * starts the settings toolbar, handles navigation,
 * and loads the SettingsFragment.
 */

package com.comp380.keeppace.settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.comp380.keeppace.R;
import com.google.android.material.appbar.MaterialToolbar;

public class MySettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // back arrow

        // Load Settings Fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsContainer, new SettingsFragment())
                .commit();
    }
}