package com.comp380.keeppace;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;


public class MyRun extends Fragment {

    private static final String TAG = "MyRun";
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private static final double TARGET_SPEED_MPS = 0.8; //
    private static final double SPEED_TOLERANCE_MPS = 0.3; // allowed +/- range


    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isUpdatingLocation = false;
    private boolean isPaused = false;

    // UI views (match XML IDs)
    private TextView textLat;
    private TextView textLng;
    private TextView textSpeed;
    private TextView textStatus;
    private TextView textTimer;
    private View rootLayout;
    private Button buttonStartLocation;
    private Button buttonPause;
    private Button buttonIncrementScore;
    private MapView map;
    private Marker userMarker;
    private Polyline pathOverlay;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0L;
    private long elapsedTime = 0L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //важно:ใส่ก่อน setContentView
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Init location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Connect UI elements
        textLat = view.findViewById(R.id.textLat);
        textLng = view.findViewById(R.id.textLng);
        textSpeed = view.findViewById(R.id.textSpeed);
        textStatus = view.findViewById(R.id.textStatus);
        textTimer = view.findViewById(R.id.textTimer);
        rootLayout = view.findViewById(R.id.rootLayout);
        buttonStartLocation = view.findViewById(R.id.buttonStartLocation);
        buttonPause = view.findViewById(R.id.buttonPause);
        buttonIncrementScore = view.findViewById(R.id.buttonIncrementScore);
        map = view.findViewById(R.id.map);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(20.0);

        pathOverlay = new Polyline();
        pathOverlay.setColor(Color.BLUE);
        pathOverlay.getPaint().setStrokeWidth(8);
        map.getOverlays().add(pathOverlay);

        userMarker = new Marker(map);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        userMarker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_online));
        map.getOverlays().add(userMarker);

        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: -");
        textTimer.setText("Time: 00:00");

        timerHandler = new Handler();

        buttonPause.setVisibility(View.GONE);

        // Increment Firestore score
        buttonIncrementScore.setOnClickListener(v -> incrementUserScore());

        // Define what happens when we get location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || isPaused) return;

                for (Location location : locationResult.getLocations()) {
                    updateUIWithLocation(location);
                }
            }
        };

        // Start/stop location updates when button is clicked
        buttonStartLocation.setOnClickListener(v -> {
            if (!isUpdatingLocation) {
                checkPermissionAndStartLocation();
            } else {
                stopLocationUpdates();
            }
        });

        buttonPause.setOnClickListener(v -> {
            if (isPaused) {
                resumeRun();
            } else {
                pauseRun();
            }
        });

        return view;

    }

    private void incrementUserScore() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No user logged in. Cannot update score.");
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid)
                .update("score", FieldValue.increment(1))
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Score incremented by 1"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to increment score", e));
    }

    private void setPaceColor(int color) {
        // Fragment root
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(color);
        }

        if (getActivity() != null) {
            // Activity root background (behind everything, including bottom nav)
            View activityRoot = getActivity().findViewById(R.id.activityRoot);
            if (activityRoot != null) {
                activityRoot.setBackgroundColor(color);
            }

            // Bottom navigation bar
            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setBackgroundColor(color);
            }

            // System bars (status + Android nav bar)
            try {
                requireActivity().getWindow().setStatusBarColor(color);
                requireActivity().getWindow().setNavigationBarColor(color);
            } catch (Exception e) {
                Log.w(TAG, "Could not set system bar colors", e);
            }
        }
    }




    // --------- LOCATION METHODS ---------

    private void checkPermissionAndStartLocation() {
        boolean hasFineLocation = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!hasFineLocation) {
            // Request permission from this Fragment
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L // every 1 second
        )
                .setMinUpdateIntervalMillis(500L) // fastest
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    requireActivity().getMainLooper()
            );
            isUpdatingLocation = true;
            buttonStartLocation.setText("Stop Run");
            buttonPause.setVisibility(View.VISIBLE);
            buttonPause.setText("Pause");

            startTime = System.currentTimeMillis();
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    long millis = System.currentTimeMillis() - startTime;
                    elapsedTime = millis;
                    int seconds = (int) (millis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;

                    textTimer.setText(String.format("Time: %02d:%02d", minutes, seconds));

                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(timerRunnable);

            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing when starting updates", e);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isUpdatingLocation = false;
        isPaused = false;
        buttonStartLocation.setText("Start Location");
        buttonPause.setVisibility(View.GONE);

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Reset UI
        setPaceColor(Color.TRANSPARENT);
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: -");
        textTimer.setText("Time: 00:00");

        pathOverlay.getPoints().clear();
        map.invalidate();

        Log.d(TAG, "Location updates stopped and UI reset");
    }

    private void pauseRun() {
        if (isUpdatingLocation && !isPaused) {
            isPaused = true;
            buttonPause.setText("Resume");
            if (timerHandler != null && timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
            }
            Log.d(TAG, "Run paused");
        }
    }

    private void resumeRun() {
        if (isUpdatingLocation && isPaused) {
            isPaused = false;
            buttonPause.setText("Pause");
            startTime = System.currentTimeMillis() - elapsedTime;
            timerHandler.post(timerRunnable);
            Log.d(TAG, "Run resumed");
        }
    }

    private void updateUIWithLocation(Location location) {
        if (location == null) {
            Log.w(TAG, "updateUIWithLocation called with null location");
            return;
        }

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.hasSpeed() ? location.getSpeed() : 0f;

        textLat.setText("Lat: " + lat);
        textLng.setText("Lng: " + lng);
        textSpeed.setText("Speed (m/s): " + speed);

        GeoPoint userLocation = new GeoPoint(lat, lng);
        map.getController().setCenter(userLocation);
        userMarker.setPosition(userLocation);

        pathOverlay.getPoints().add(userLocation);

        map.invalidate(); // Redraw the map

        // Simple pace feedback
        String statusText;
        if (speed <= 0.1f) {
            statusText = "Not moving";
        } else {
            double diff = speed - TARGET_SPEED_MPS;

            if (diff > SPEED_TOLERANCE_MPS) {
                statusText = "Too fast";
            } else if (diff < -SPEED_TOLERANCE_MPS) {
                statusText = "Too slow";
            } else {
                statusText = "On pace";
            }
        }

        //(0xFF2bc335);  // GREEN
        //(0xFF9E9E9E);  // GRAY
        //(0xFFac2121);  // RED

        textStatus.setText("Status: " + statusText);

        switch (statusText) {
            case "On pace":
                setPaceColor(0xFF2bc335);  // GREEN
                break;

            case "Not moving":
                setPaceColor(0xFF9E9E9E);  // GRAY
                break;

            default: // Too fast / Too slow
                setPaceColor(0xFFac2121);  // RED
                break;
        }


        Log.d(TAG, "updateUIWithLocation: lat=" + lat
                + " lng=" + lng + " speed=" + speed + " status=" + statusText);
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isUpdatingLocation) {
            stopLocationUpdates();
        }
        if (map != null) {
            map.onDetach();
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    // Handle permission result for this Fragment
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Log.w(TAG, "Location permission denied");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}
