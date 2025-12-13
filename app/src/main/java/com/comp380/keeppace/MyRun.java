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

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.content.Context;

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
    private EditText inputTargetPace;
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
    private final ArrayList<Double> recordedLats = new ArrayList<>();
    private final ArrayList<Double> recordedLngs = new ArrayList<>();

    // Fragment view-state flag
    private boolean viewIsAlive = false;

    private String lastPaceStatus = "Ready";

    private long lastBuzzTime = 0;
    private static final long BUZZ_COOLDOWN_MS = 3000; // 3 seconds



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // REQUIRED for OSMDroid
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
        );

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);
        viewIsAlive = true;

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // UI Binding
        textLat = view.findViewById(R.id.textLat);
        textLng = view.findViewById(R.id.textLng);
        textSpeed = view.findViewById(R.id.textSpeed);
        textStatus = view.findViewById(R.id.textStatus);
        textTimer = view.findViewById(R.id.textTimer);
        inputTargetPace = view.findViewById(R.id.inputTargetPace);
        rootLayout = view.findViewById(R.id.rootLayout);
        buttonStartLocation = view.findViewById(R.id.buttonStartLocation);
        buttonPause = view.findViewById(R.id.buttonPause);
        Button buttonIncrementScore = view.findViewById(R.id.buttonIncrementScore);
        map = view.findViewById(R.id.map);

        // IMPORTANT: prevent osmdroid from fully destroying the map when off-screen (ViewPager2 fix)
        if (map != null) {
            map.setDestroyMode(false); // <-- KEY FIX
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(18.0);
            map.getController().setCenter(new GeoPoint(34.2478, -118.4390)); // Default center
        }

        // GPS Overlay
        if (map != null) {
            myLocationOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(requireContext()), map);
            myLocationOverlay.setDrawAccuracyEnabled(true);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            map.getOverlays().add(myLocationOverlay);
        }

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
            if (minutesPerMile > 0) {
                targetSpeedMps = 1609.34 / (minutesPerMile * 60);
                Log.d(TAG, "Target Speed updated to: " + targetSpeedMps + " m/s");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid Pace Format", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        updateTargetPace();
        inputTargetPace.setEnabled(false);

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

            if (map != null) {
                pathOverlay = new Polyline();
                pathOverlay.setColor(Color.BLUE);
                pathOverlay.getPaint().setStrokeWidth(10);
                map.getOverlays().add(pathOverlay);
            }

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
                Toast.makeText(requireContext(),
                        "Location permission is required to show your position",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Save run if it was actually a real run
        saveRunToFirestore();

        isUpdatingLocation = false;
        isPaused = false;

        elapsedTime = 0L;
        totalDistanceMeters = 0.0;
        lastLocation = null;
        inputTargetPace.setEnabled(true);

        buttonStartLocation.setText("START RUN");
        buttonPause.setVisibility(View.GONE);

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (map != null && pathOverlay != null) {
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
        if (user == null || totalDistanceMeters < 10) return;

        double miles = totalDistanceMeters * 0.000621371;
        int pointsEarned = (int) miles;

        Map<String, Object> runData = new HashMap<>();
        runData.put("timestamp", FieldValue.serverTimestamp());
        runData.put("distanceMeters", totalDistanceMeters);
        runData.put("timeMillis", elapsedTime);
        runData.put("points", pointsEarned);
        runData.put("pathLats", recordedLats);
        runData.put("pathLngs", recordedLngs);

        db.collection("users").document(user.getUid())
                .collection("runs")
                .add(runData);

        updateUserTotalScore(pointsEarned);

        Toast.makeText(requireContext(),
                String.format("Run Saved! %.2f mi (+%d pts)", miles, pointsEarned),
                Toast.LENGTH_LONG).show();
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
            if (timerHandler != null && timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
            }
        }
    }

    private void resumeRun() {
        if (isUpdatingLocation && isPaused) {
            isPaused = false;
            buttonPause.setText("Pause");
            startTime = System.currentTimeMillis() - elapsedTime;
            if (timerHandler != null && timerRunnable != null) {
                timerHandler.post(timerRunnable);
            }
        }
    }

    private void updateUIWithLocation(Location location) {
        if (location == null) return;
        if (!viewIsAlive || !isAdded() || getView() == null) return;
        if (map == null || textLat == null || textLng == null ||
                textSpeed == null || textStatus == null || textTimer == null) {
            return;
        }

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float speed = location.hasSpeed() ? location.getSpeed() : 0f;

        if (lastLocation != null) {
            double distanceGap = lastLocation.distanceTo(location);
            if (distanceGap > 2.0) {
                totalDistanceMeters += distanceGap;
            }
        }
        lastLocation = location;

        recordedLats.add(lat);
        recordedLngs.add(lng);

        textLat.setText(String.format("Lat: %.5f", lat));
        textLng.setText(String.format("Lng: %.5f", lng));
        textSpeed.setText(String.format("%.2f m/s", speed));

        double miles = totalDistanceMeters * 0.000621371;
        // String distanceString = String.format("%.2f mi", miles); // Use if you display distance

        GeoPoint userLocation = new GeoPoint(lat, lng);
        if (pathOverlay != null && pathOverlay.getPoints().isEmpty()) {
            map.getController().setCenter(userLocation);
        }
        if (pathOverlay != null) {
            pathOverlay.getPoints().add(userLocation);
        }
        map.invalidate();

        String statusText;
        if (speed <= 0.1f) {
            statusText = "Not moving";
        } else {
            double diff = speed - targetSpeedMps;
            if (diff > SPEED_TOLERANCE_MPS) statusText = "Too fast";
            else if (diff < -SPEED_TOLERANCE_MPS) statusText = "Too slow";
            else statusText = "On pace";
        }

        textStatus.setText(statusText);

        switch (statusText) {
            case "On pace":
                setPaceColor(0xFF2bc335); // Green
                break;
            case "Not moving":
                setPaceColor(0xFF9E9E9E); // Grey
                break;
            default:
                setPaceColor(0xFFac2121); // Red
                break;
        }

        textStatus.setText(statusText);

        if (statusText.equals("Too slow")) {
            long now = System.currentTimeMillis();
            if (now - lastBuzzTime >= BUZZ_COOLDOWN_MS) {
                buzz();
                lastBuzzTime = now;
            }
        }
        // Too fast - buzz every update
        else if (statusText.equals("Too fast")) {
            buzz();
        }

        lastPaceStatus = statusText;


    }
    private void buzz() {
        Context ctx = getContext();
        if (ctx == null) return;

        try {
            Vibrator vibrator;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm == null) {
                    Log.w(TAG, "VibratorManager is null, cannot buzz");
                    return;
                }
                vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator == null || !vibrator.hasVibrator()) {
                Log.w(TAG, "No vibrator on this device");
                return;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(
                        150,
                        VibrationEffect.DEFAULT_AMPLITUDE
                );
                vibrator.vibrate(effect);
            } else {
                // Deprecated on newer APIs but fine for old ones
                vibrator.vibrate(150);
            }

        } catch (SecurityException e) {
            Log.w(TAG, "Missing VIBRATE permission, skipping buzz", e);
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
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
        }
    }

    @Override
    public void onPause() {
        if (map != null) {
            map.onPause();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
            myLocationOverlay.disableFollowLocation();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {

        stopLocationUpdates();
        viewIsAlive = false;


        super.onDestroyView();
    }
}
