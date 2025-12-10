package com.comp380.keeppace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class HomePage extends AppCompatActivity {
    BottomNavigationView bottomNav;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        UserFirestoreHelper.ensureUserDocumentExists();

        // ViewPager
        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        // BottomNavigationView
        bottomNav = findViewById(R.id.bottomNav);

        // Set the first launch page as My Run
        viewPager.setCurrentItem(1, false);

        // When tapping bottom nav switch pages
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_leaderboard) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_myrun) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        // When swiping update bottom nav highlight
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_leaderboard);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_myrun);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });

        //Floating Action button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(HomePage.this, v);
            popup.getMenuInflater().inflate(R.menu.fab_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();


                if (id == R.id.menu_settings) {
                    startActivity(new Intent(this, MySettingsActivity.class));
                    return true;
                }

                if (id == R.id.menu_signout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }

                return false;
            });
            popup.show();
        });
    }
}
