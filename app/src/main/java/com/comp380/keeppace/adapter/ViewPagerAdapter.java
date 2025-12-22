/**
 * ViewPagerAdapter manages the fragments displayed in the main ViewPager2.
 *
 * Position 0 -> Leaderboard
 * Position 1 -> MyRun
 * Position 2 -> ProfilePage
 */
package com.comp380.keeppace.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.comp380.keeppace.leaderboard.Leaderboard;
import com.comp380.keeppace.profile.ProfilePage;
import com.comp380.keeppace.run.MyRun;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0: return new Leaderboard();
            case 1: return new MyRun();
            case 2: return new ProfilePage();
            default: return new MyRun();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
