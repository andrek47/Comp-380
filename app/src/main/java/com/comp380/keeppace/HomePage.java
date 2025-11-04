package com.comp380.keeppace;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class HomePage extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "You have signed out.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomePage.this, EmailPasswordActivity.class);
            startActivity(intent);
        });


    }
}