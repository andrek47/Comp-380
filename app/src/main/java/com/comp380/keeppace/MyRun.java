package com.comp380.keeppace;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MyRun extends Fragment {

    private static final String TAG = "MyRun";
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    // Pace Logic
    private double targetSpeedMps = 2.68; // Default ~10 min/mile
    private static final double SPEED_TOLERANCE_MPS = 0.5;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isUpdatingLocation = false;
    private boolean isPaused = false;

    // UI
    private TextView textLat, textLng, textSpeed, textStatus, textTimer;
    private EditText inputTargetPace; // Renamed to match your likely XML
    private View rootLayout;
    private Button buttonStartLocation, buttonPause;
    private MapView map;

    // Map Overlays
    private Polyline pathOverlay;
    private MyLocationNewOverlay myLocationOverlay;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0L;
    private long elapsedTime = 0L;

    // Distance Tracking (Missing from your older code)
    private double totalDistanceMeters = 0.0;
    private Location lastLocation = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 1. OSMDroid Config
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // 2. Bind UI (FIXED: Changed ID to inputTargetPace)
        textLat = view.findViewById(R.id.textLat);
        textLng = view.findViewById(R.id.textLng);
        textSpeed = view.findViewById(R.id.textSpeed);
        textStatus = view.findViewById(R.id.textStatus);
        textTimer = view.findViewById(R.id.textTimer);
        rootLayout = view.findViewById(R.id.rootLayout);
        buttonStartLocation = view.findViewById(R.id.buttonStartLocation);
        buttonPause = view.findViewById(R.id.buttonPause);
        Button buttonIncrementScore = view.findViewById(R.id.buttonIncrementScore);
        map = view.findViewById(R.id.map);
        inputTargetPace = view.findViewById(R.id.inputTargetPace); // Matches XML ID

        // 3. Map Setup
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);
        // Default Start Point (CSUN Area) - Prevents blank grid
        map.getController().setCenter(new GeoPoint(34.2415, -118.5277));

        // 4. "Blue Dot" Overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);

        resetUI();

        timerHandler = new Handler(Looper.getMainLooper());
        buttonPause.setVisibility(View.GONE);
        buttonIncrementScore.setOnClickListener(v -> incrementUserScore());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (isPaused) return;
                for (Location location : locationResult.getLocations()) {
                    updateUIWithLocation(location);
                }
            }
        };

        buttonStartLocation.setOnClickListener(v -> {
            if (!isUpdatingLocation) {
                // Handle the Pace Input
                updateTargetPace();
                checkPermissionAndStartLocation();
            } else {
                stopLocationUpdates();
            }
        });

        buttonPause.setOnClickListener(v -> {
            if (isPaused) resumeRun();
            else pauseRun();
        });

        return view;
    }

    // --- LIFECYCLE FIX (Prevents Map Disappearing) ---
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onDestroyView() {
        // Stop location to save battery
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        // FIX: DO NOT CALL map.onDetach() HERE. It breaks the app.
        // Just let onPause handle it.
        super.onDestroyView();
    }

    // --- PERMISSIONS & UPDATES ---

    private void checkPermissionAndStartLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        // FIX: Remove "MinUpdateDistance" so it works indoors/standing still
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(0f) // Important for testing!
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            isUpdatingLocation = true;

            // UI Updates
            buttonStartLocation.setText("Stop Run");
            buttonStartLocation.setBackgroundColor(Color.RED);
            buttonPause.setVisibility(View.VISIBLE);
            buttonPause.setText("Pause");
            if(inputTargetPace != null) inputTargetPace.setEnabled(false);

            // Path Setup
            pathOverlay = new Polyline();
            pathOverlay.setColor(Color.BLUE);
            pathOverlay.getPaint().setStrokeWidth(10);
            map.getOverlays().add(pathOverlay);

            // Reset Data
            totalDistanceMeters = 0.0;
            elapsedTime = 0L;
            lastLocation = null;
            startTime = System.currentTimeMillis();

            // Timer Logic
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    long millis = System.currentTimeMillis() - startTime;
                    elapsedTime = millis;
                    int seconds = (int) (millis / 1000);
                    int minutes = seconds / 60;
                    seconds %= 60;
                    textTimer.setText(String.format("%02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(timerRunnable);

        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isUpdatingLocation = false;
        isPaused = false;

        // Save Score
        saveRunToFirestore();

        buttonStartLocation.setText("Start Run");
        buttonStartLocation.setBackgroundColor(Color.GREEN); // Or your default color
        buttonPause.setVisibility(View.GONE);
        if(inputTargetPace != null) inputTargetPace.setEnabled(true);

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        if (pathOverlay != null) {
            map.getOverlays().remove(pathOverlay);
            pathOverlay = null;
        }

        map.invalidate();
        resetUI();
    }

    private void updateUIWithLocation(Location location) {
        if (location == null) return;

        // FIX: The "Ocean Jump" Filter
        if (Math.abs(location.getLatitude()) < 0.001 && Math.abs(location.getLongitude()) < 0.001) {
            return;
        }

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.hasSpeed() ? location.getSpeed() : 0f;

        // Calculate Distance
        if (lastLocation != null) {
            totalDistanceMeters += lastLocation.distanceTo(location);
        }
        lastLocation = location;

        // Map Updates
        GeoPoint userLoc = new GeoPoint(lat, lng);
        map.getController().animateTo(userLoc); // Follow user

        if (pathOverlay != null) {
            pathOverlay.addPoint(userLoc);
        }
        map.invalidate();

        // Status Text
        textLat.setText(String.format("Lat: %.5f", lat));
        textLng.setText(String.format("Lng: %.5f", lng));
        textSpeed.setText(String.format("%.2f m/s", speed));

        double miles = totalDistanceMeters * 0.000621371;

        String statusText;
        if (speed <= 0.1f) {
            statusText = "Not moving";
        } else {
            double diff = speed - targetSpeedMps;
            if (diff > SPEED_TOLERANCE_MPS) statusText = "Too fast";
            else if (diff < -SPEED_TOLERANCE_MPS) statusText = "Too slow";
            else statusText = "On pace";
        }
        textStatus.setText(String.format("%.2f mi • %s", miles, statusText));

        // Colors
        int color;
        switch (statusText) {
            case "On pace": color = 0xFF2bc335; break; // Green
            case "Not moving": color = 0xFF9E9E9E; break; // Gray
            default: color = 0xFFac2121; break; // Red
        }
        setPaceColor(color);
    }

    // --- HELPER FUNCTIONS ---

    private void pauseRun() {
        if (isUpdatingLocation && !isPaused) {
            isPaused = true;
            buttonPause.setText("Resume");
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resumeRun() {
        if (isUpdatingLocation && isPaused) {
            isPaused = false;
            buttonPause.setText("Pause");
            // Reset timer start time to account for pause duration
            startTime = System.currentTimeMillis() - elapsedTime;
            timerHandler.post(timerRunnable);
        }
    }

    private void updateTargetPace() {
        if (inputTargetPace == null) return;
        String input = inputTargetPace.getText().toString();
        if (input.isEmpty()) return;
        try {
            // Simplified: Treats input "10" as 10 min/mile
            double minutesPerMile = Double.parseDouble(input);
            if (minutesPerMile > 0) {
                targetSpeedMps = 1609.34 / (minutesPerMile * 60);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void resetUI() {
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("0.00 m/s");
        textStatus.setText("Ready");
        textTimer.setText("00:00");
        setPaceColor(Color.TRANSPARENT);
    }

    private void setPaceColor(int color) {
        if (rootLayout != null) rootLayout.setBackgroundColor(color);
        if (getActivity() != null) {
            try {
                requireActivity().getWindow().setStatusBarColor(color);
            } catch (Exception ignored) {}
        }
    }

    private void incrementUserScore() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("score", FieldValue.increment(1));
        }
    }

    private void saveRunToFirestore() {
        // Basic save logic
        if (mAuth.getCurrentUser() != null && totalDistanceMeters > 10) {
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("score", FieldValue.increment((int)(totalDistanceMeters / 100)));
            Toast.makeText(getContext(), "Run Saved!", Toast.LENGTH_SHORT).show();
        }
    }
}