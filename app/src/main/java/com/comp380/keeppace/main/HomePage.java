/**
 * HomePage is the screen you see after logging in
 * It uses a ViewPager2 + BottomNavigationView to switch between app tabs
 * (Leaderboard, My Run, Profile), it also
 * provides a floating action button menu for Settings and Sign Out.
 */

package com.comp380.keeppace.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.comp380.keeppace.settings.MySettingsActivity;
import com.comp380.keeppace.R;
import com.comp380.keeppace.model.UserFirestoreHelper;
import com.comp380.keeppace.adapter.ViewPagerAdapter;
import com.comp380.keeppace.auth.AuthHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        FloatingActionButton fabAction2 = findViewById(R.id.fabAction2);
        FloatingActionButton fabAction3 = findViewById(R.id.fabAction3);

        boolean[] isOpen = {false};
        fab.setOnClickListener(v -> {
            if (!isOpen[0]) {
                // Open speed-dial
                showFab(fabAction2, 1);
                showFab(fabAction3, 2);

                fab.animate().rotation(45f).setDuration(200); // turn into X
                isOpen[0] = true;

            } else {
                // Close speed-dial
                hideFab(fabAction2);
                hideFab(fabAction3);


                fab.animate().rotation(0f).setDuration(200);
                isOpen[0] = false;
            }
        });
        fabAction2.setOnClickListener(v -> {
            startActivity(new Intent(this, MySettingsActivity.class));

        });
        fabAction3.setOnClickListener(v -> {
            AuthHelper.signOut(this);
        });
    }
    private void showFab(FloatingActionButton miniFab, int index) {
        miniFab.setVisibility(View.VISIBLE);
        miniFab.setAlpha(0f);
        miniFab.setScaleX(0f);
        miniFab.setScaleY(0f);
        miniFab.setTranslationX(-55);

        miniFab.animate()
                .translationY(index * 180)   // distance between fabs
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
    }

    private void hideFab(FloatingActionButton miniFab) {
        miniFab.animate()
                .translationY(0)
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(200)
                .withEndAction(() -> miniFab.setVisibility(View.GONE))
                .start();
    }

    }



