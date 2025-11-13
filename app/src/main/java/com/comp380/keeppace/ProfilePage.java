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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);
        //make an username out of email
        TextView username = view.findViewById(R.id.username);

        //check if there is an user
        if (user != null) {
            String email = user.getEmail(); // get user email
            String name = email != null ? email.split("@")[0] : "Guest"; // split the email to only get till @

            username.setText(name.substring(0,1).toUpperCase() + name.substring(1)); //set the username
        } else {
            username.setText("Email: Not signed in"); //not logged in
        }

        return view;
    }
}