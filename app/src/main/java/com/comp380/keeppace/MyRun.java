package com.comp380.keeppace;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    // Pace Logic Variables
    private double targetSpeedMps = 2.68; // Default approx 10 min/mile
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
    private TextView textLat;
    private TextView textLng;
    private TextView textSpeed;
    private TextView textStatus;
    private TextView textTimer;
    private EditText inputTargetPace; // New Input Field
    private View rootLayout;
    private Button buttonStartLocation;
    private Button buttonPause;
    private MapView map;

    private Polyline pathOverlay;

    // OSMDroid location overlay
    private MyLocationNewOverlay myLocationOverlay;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0L;
    private long elapsedTime = 0L;

    // Tracking Variables
    private double totalDistanceMeters = 0.0;
    private Location lastLocation = null;

    // Path Recording for Map Viewer
    private ArrayList<Double> recordedLats = new ArrayList<>();
    private ArrayList<Double> recordedLngs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // REQUIRED for OSMDroid
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // UI Binding
        textLat = view.findViewById(R.id.textLat);
        textLng = view.findViewById(R.id.textLng);
        textSpeed = view.findViewById(R.id.textSpeed);
        textStatus = view.findViewById(R.id.textStatus);
        textTimer = view.findViewById(R.id.textTimer);
        inputTargetPace = view.findViewById(R.id.inputTargetPace); // Bind Input
        rootLayout = view.findViewById(R.id.rootLayout);
        buttonStartLocation = view.findViewById(R.id.buttonStartLocation);
        buttonPause = view.findViewById(R.id.buttonPause);
        Button buttonIncrementScore = view.findViewById(R.id.buttonIncrementScore);
        map = view.findViewById(R.id.map);

        // Map setup
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(34.2478, -118.4390)); // Default center

        // GPS Overlay
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(myLocationOverlay);

        // Initial UI Text
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: Ready");
        textTimer.setText("00:00");

        timerHandler = new Handler();
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

    private void incrementUserScore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid())
                .update("score", FieldValue.increment(1));
    }

    private void checkPermissionAndStartLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);

        } else {
            startLocationUpdates();
        }
    }

    private void updateTargetPace() {
        String input = inputTargetPace.getText().toString();
        if (input.isEmpty()) return;

        try {
            double minutesPerMile = Double.parseDouble(input);
            // Conversion: 1 mile = 1609.34 meters
            // Speed = Distance / Time (seconds)
            if (minutesPerMile > 0) {
                targetSpeedMps = 1609.34 / (minutesPerMile * 60);
                Log.d(TAG, "Target Speed updated to: " + targetSpeedMps + " m/s");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid Pace Format", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        // 1. Set Pace from Input
        updateTargetPace();
        inputTargetPace.setEnabled(false); // Lock input during run

        // 2. Clear previous route data
        recordedLats.clear();
        recordedLngs.clear();

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    request, locationCallback, requireActivity().getMainLooper());

            isUpdatingLocation = true;
            buttonStartLocation.setText("Stop Run");
            buttonPause.setVisibility(View.VISIBLE);
            buttonPause.setText("Pause");

            pathOverlay = new Polyline();
            pathOverlay.setColor(Color.BLUE);
            pathOverlay.getPaint().setStrokeWidth(10);
            map.getOverlays().add(pathOverlay);

            startTime = System.currentTimeMillis() - elapsedTime;

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

        } catch (SecurityException ignored) {}
    }

    private void stopLocationUpdates() {
        // 1. SAVE THE RUN (Calculate Points & Save Path)
        saveRunToFirestore();

        // 2. Stop Sensors
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isUpdatingLocation = false;
        isPaused = false;

        // 3. Reset UI & Variables
        elapsedTime = 0L;
        totalDistanceMeters = 0.0;
        lastLocation = null;
        inputTargetPace.setEnabled(true); // Unlock input

        buttonStartLocation.setText("START RUN");
        buttonPause.setVisibility(View.GONE);

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (pathOverlay != null) {
            map.getOverlays().remove(pathOverlay);
            pathOverlay = null;
            map.invalidate();
        }

        setPaceColor(Color.TRANSPARENT);
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("0.00 m/s");
        textStatus.setText("Status: Ready");
        textTimer.setText("00:00");
    }

    private void saveRunToFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        // Don't save if less than 10 meters (accidental click)
        if (user == null || totalDistanceMeters < 10) {
            return;
        }

        // CONVERSION: Meters to Miles
        double miles = totalDistanceMeters * 0.000621371;

        // CALCULATE POINTS: 1 Point per Mile
        int pointsEarned = (int) miles;

        // Prepare Data
        Map<String, Object> runData = new HashMap<>();
        runData.put("timestamp", FieldValue.serverTimestamp());
        runData.put("distanceMeters", totalDistanceMeters);
        runData.put("timeMillis", elapsedTime);
        runData.put("points", pointsEarned);

        // Save the Route for Map Viewer
        runData.put("pathLats", recordedLats);
        runData.put("pathLngs", recordedLngs);

        // 1. Save to History
        db.collection("users").document(user.getUid())
                .collection("runs")
                .add(runData);

        // 2. Update Total Score
        updateUserTotalScore(pointsEarned);

        Toast.makeText(requireContext(), String.format("Run Saved! %.2f mi (+%d pts)", miles, pointsEarned), Toast.LENGTH_LONG).show();
    }

    private void updateUserTotalScore(int pointsToAdd) {
        if (pointsToAdd == 0) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("score", FieldValue.increment(pointsToAdd))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating score", e));
        }
    }

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

            startTime = System.currentTimeMillis() - elapsedTime;
            timerHandler.post(timerRunnable);
        }
    }

    private void updateUIWithLocation(Location location) {
        if (location == null) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.hasSpeed() ? location.getSpeed() : 0f;

        // Calculate Distance
        if (lastLocation != null) {
            double distanceGap = lastLocation.distanceTo(location);
            if (distanceGap > 2.0) {
                totalDistanceMeters += distanceGap;
            }
        }
        lastLocation = location;

        // Record Path Points
        recordedLats.add(lat);
        recordedLngs.add(lng);

        textLat.setText(String.format("Lat: %.5f", lat));
        textLng.setText(String.format("Lng: %.5f", lng));
        textSpeed.setText(String.format("%.2f m/s", speed));

        // SHOW DISTANCE IN MILES
        double miles = totalDistanceMeters * 0.000621371;
        String distanceString = String.format("%.2f mi", miles);

        // Map Drawing
        GeoPoint userLocation = new GeoPoint(lat, lng);
        if (pathOverlay != null && pathOverlay.getPoints().isEmpty()) {
            map.getController().setCenter(userLocation);
        }
        if (pathOverlay != null) {
            pathOverlay.getPoints().add(userLocation);
        }
        map.invalidate();

        // Pace Logic (Using Variable targetSpeedMps)
        String statusText;
        if (speed <= 0.1f) {
            statusText = "Not moving";
        } else {
            double diff = speed - targetSpeedMps; // Dynamic check
            if (diff > SPEED_TOLERANCE_MPS) statusText = "Too fast";
            else if (diff < -SPEED_TOLERANCE_MPS) statusText = "Too slow";
            else statusText = "On pace";
        }

        textStatus.setText(statusText); // Just status, distance is elsewhere if needed

        switch (statusText) {
            case "On pace": setPaceColor(0xFF2bc335); break; // Green
            case "Not moving": setPaceColor(0xFF9E9E9E); break; // Grey
            default: setPaceColor(0xFFac2121); break; // Red
        }
    }

    private void setPaceColor(int color) {
        if (rootLayout != null) rootLayout.setBackgroundColor(color);

        if (getActivity() != null) {
            View activityRoot = getActivity().findViewById(R.id.activityRoot);
            if (activityRoot != null) activityRoot.setBackgroundColor(color);

            View bottomNav = getActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setBackgroundColor(color);

            try {
                requireActivity().getWindow().setStatusBarColor(color);
                requireActivity().getWindow().setNavigationBarColor(color);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isUpdatingLocation) stopLocationUpdates();
        if (map != null) map.onDetach();
    }

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
}