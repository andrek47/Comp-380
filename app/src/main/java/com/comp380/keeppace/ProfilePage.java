package com.comp380.keeppace;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ProfilePage extends Fragment {

    private TextView username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);
        username = view.findViewById(R.id.username);

        if (user != null) {

            String name = user.getDisplayName();
            String email = user.getEmail();

            username.setText(email != null ? email : "Email: (none");
        } else {
            username.setText("Email: Not signed in");
        }

        // Inflate the layout for this fragment
        return view;
    }
}