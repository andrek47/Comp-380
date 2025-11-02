package com.example.keeppace; // make sure this matches your package name

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
//import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_main); // connects to the XML layout

       // Button goToHomePage = findViewById(R.id.goToHomePage);
        Button goToAuth = findViewById(R.id.goToAuth);

        /*goToHomePage.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,HomePage.class);
            startActivity(intent);
        } );*/

        TextView signUpText = findViewById(R.id.signUpText);

        String text = "New To Keep Pace? Sign Up";
        SpannableString spannable = new SpannableString(text);

        ClickableSpan signUpClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                Intent intent = new Intent(MainActivity.this, UserCreation.class);
                startActivity(intent);
            }
        };

        int start = text.indexOf("Sign Up");
        int end = start + "Sign Up".length();

        spannable.setSpan(signUpClick, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signUpText.setText(spannable);
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());
        signUpText.setHighlightColor(Color.TRANSPARENT);

        goToAuth.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, EmailPasswordActivity.class);
            startActivity(intent);
        });

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

       // Button testButton = findViewById(R.id.testButton);
        //testButton.setOnClickListener(v -> {
          //  Bundle bundle = new Bundle();
            //bundle.putString(FirebaseAnalytics.Param.METHOD, "button_click");
            //firebaseAnalytics.logEvent("test_firebase_event", bundle);
            //Toast.makeText(MainActivity.this, "Firebase event logged!", Toast.LENGTH_SHORT).show();
       //});
        FirebaseAuth.getInstance().signOut();


    }
}
