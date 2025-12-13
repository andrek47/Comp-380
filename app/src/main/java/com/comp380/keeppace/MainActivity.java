/**
        Checks whether a user is already logged with Firebase.
        *  - If a user is logged in, then direct to the HomePage.
        *  - If not, shows a start button which leads to the LoginActivity.
 */
package com.comp380.keeppace; // make sure this matches your package name

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics firebaseAnalytics;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // connects to the XML layout


        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        //Start Button
        Button startButton = findViewById(R.id.startButton);
        if(currentUser == null) {
            startButton.setOnClickListener(view -> {
                Intent intent = new Intent(this, LoginActivity.class); //directs to login/signup page
                Toast.makeText(this, "Welcome to KeepPace", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            });
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this); // analytics

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //Checks if there is a user already logged in
        if (user != null) {
            //username from email
            String email = user.getEmail();
            String username = email != null ? email.split("@")[0] : "Guest";

            Log.d("MainActivity", "User logged in: " + user.getEmail());

            //welcome back comment with the username
            Toast.makeText(this, "Welcome Back " + username, Toast.LENGTH_SHORT).show();

            //redirects straight to HomePage.class if there is already an account logged in
            startActivity(new Intent(this, HomePage.class));
            finish();
        } else {
            //error checking
            Log.d("MainActivity", "No user logged in — stay on main screen");
        }
    }

}
