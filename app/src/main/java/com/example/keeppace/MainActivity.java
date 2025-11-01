package com.example.keeppace; // make sure this matches your package name

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // connects to the XML layout

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Button testButton = findViewById(R.id.testButton);
        testButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.METHOD, "button_click");
            firebaseAnalytics.logEvent("test_firebase_event", bundle);
            Toast.makeText(MainActivity.this, "Firebase event logged!", Toast.LENGTH_SHORT).show();
        });
    }
}
