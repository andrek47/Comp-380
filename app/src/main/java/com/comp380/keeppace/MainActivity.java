package com.comp380.keeppace; // make sure this matches your package name

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
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

//import com.google.firebase.auth.AuthCredential;
//import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics firebaseAnalytics;

    private FirebaseUser mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // connects to the XML layout

        mAuth = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseUser currentUser = mAuth;

        //Login Button
        Button goToAuth = findViewById(R.id.goToAuth);
        if(currentUser == null) {
            goToAuth.setOnClickListener(view -> {
                Intent intent = new Intent(this, EmailPasswordActivity.class);
                Toast.makeText(this, "Welcome to KeepPace", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            });
        }

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


        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //FirebaseAuth.getInstance().signOut();

    }

    @Override
    protected void onStart(){
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            startActivity(new Intent(this, HomePage.class));
            finish();
        }
    }
}
