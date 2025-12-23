/**
 * LoginActivity handles user authentication.
 * Uses tab-based login and signup screens,
 * it also manages navigation between authentication,
 * and redirects authenticated users to the HomePage.
 */

package com.comp380.keeppace.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.comp380.keeppace.adapter.LoginAdapter;
import com.comp380.keeppace.main.HomePage;
import com.comp380.keeppace.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager2;
    LoginAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //tabs for fragments
        setContentView(R.layout.activity_login);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager2 = findViewById(R.id.viewPager);
        viewPagerAdapter = new LoginAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);

        //back button to go back to previous page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

        //more tab stuff
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.getTabAt(position).select();
            }
        });
    }
    private FirebaseAuth.AuthStateListener authListener;

    /*@Override
    protected void onStart() {
        super.onStart();

        authListener = auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                Log.d("AUTH", "User logged in, navigating");
                AuthHelper.goToHome(this);
            }
        };

        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("AUTH_GATE", "onResume user = " + user);

        if (user != null) {
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AuthHelper.onActivityResult(requestCode, resultCode, data);
        AuthHelper.signInHelper(this, requestCode, resultCode, data);
    }

}
