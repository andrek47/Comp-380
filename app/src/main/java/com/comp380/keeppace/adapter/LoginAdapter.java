package com.comp380.keeppace.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.comp380.keeppace.auth.LoginFragment;
import com.comp380.keeppace.auth.SignUpFragment;
import com.comp380.keeppace.run.MyRun;

public class LoginAdapter extends FragmentStateAdapter {



    public LoginAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0: return new LoginFragment();
            case 1: return new SignUpFragment();
            default: return new MyRun();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}