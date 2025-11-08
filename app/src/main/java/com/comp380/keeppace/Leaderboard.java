package com.comp380.keeppace;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;


import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Leaderboard extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView( LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // Example: manually setting text in the table
        TextView name1 = view.findViewById(R.id.name1);
        TextView score1 = view.findViewById(R.id.score1);
        TextView rank1 = view.findViewById(R.id.rank1);

        TextView name2 = view.findViewById(R.id.name2);
        TextView score2 = view.findViewById(R.id.score2);
        TextView rank2 = view.findViewById(R.id.rank2);

        // Set text manually for testing
        name1.setText("Andre");
        score1.setText("1000000");
        rank1.setText("1");

        name2.setText("test");
        score2.setText("0");
        rank2.setText("2");




        TextView txtFirst = view.findViewById(R.id.txt_first);
        TextView txtSecond = view.findViewById(R.id.txt_second);
        TextView txtThird = view.findViewById(R.id.txt_third);

        txtFirst.setText("Andre");
        txtSecond.setText("Test");




        return view;
    }
}