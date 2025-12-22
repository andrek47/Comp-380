package com.comp380.keeppace.run;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.comp380.keeppace.R;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyRun extends Fragment {

    private static final String TAG = "MyRun";
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private static final double SPEED_TOLERANCE_MPS = 0.5;
    private static final long BUZZ_COOLDOWN_MS = 3000; // 3 seconds

    // Pace Logic
    private double targetSpeedMps = 2.68; // Default ~10 min/mile

    // Buzz logic
    private long lastBuzzTime = 0;

    // Fragment view-state flag
    private boolean viewIsAlive = false;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isUpdatingLocation = false;
    private boolean isPaused = false;

    // Distance Tracking
    private double totalDistanceMeters = 0.0;
    private Location lastLocation = null;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0L;
    private long elapsedTime = 0L;

    // Path Recording for Map Viewer
    private final ArrayList<Double> recordedLats = new ArrayList<>();
    private final ArrayList<Double> recordedLngs = new ArrayList<>();

    // Map Overlays
    private Polyline pathOverlay;
    private MyLocationNewOverlay myLocationOverlay;

    // UI
    private TextView textLat;
    private TextView textLng;
    private TextView textSpeed;
    private TextView textStatus;
    private TextView textTimer;
    private EditText inputTargetPace;
    private View rootLayout;
    private CardView bottomControlPanel;
    private Button buttonStartLocation, buttonPause;
    private MapView map;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // OSMDroid Config
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

        // Bind UI
        textLat = view.findViewById(R.id.textLat);
        textLng = view.findViewById(R.id.textLng);
        textSpeed = view.findViewById(R.id.textSpeed);
        textStatus = view.findViewById(R.id.textStatus);
        textTimer = view.findViewById(R.id.textTimer);
        inputTargetPace = view.findViewById(R.id.inputTargetPace);
        rootLayout = view.findViewById(R.id.rootLayout);
        bottomControlPanel = view.findViewById(R.id.bottomControlPanel);
        buttonStartLocation = view.findViewById(R.id.buttonStartLocation);
        buttonPause = view.findViewById(R.id.buttonPause);
        Button buttonIncrementScore = view.findViewById(R.id.buttonIncrementScore);
        map = view.findViewById(R.id.map);

        // Map setup + ViewPager fix
        if (map != null) {
            map.setDestroyMode(false);
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
                updateTargetPace();
                if (bottomControlPanel != null) {
                    bottomControlPanel.setCardBackgroundColor(Color.parseColor("#40FFFFFF"));
                }
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

    // ---------- Permissions & starting updates ----------

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
        inputTargetPace.setEnabled(false);

        recordedLats.clear();
        recordedLngs.clear();

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    request, locationCallback, Looper.getMainLooper());
            isUpdatingLocation = true;

            buttonStartLocation.setText("Stop Run");
            buttonStartLocation.setBackgroundColor(Color.RED);
            buttonPause.setVisibility(View.VISIBLE);
            buttonPause.setText("Pause");

            if (map != null) {
                pathOverlay = new Polyline();
                pathOverlay.setColor(Color.BLUE);
                pathOverlay.getPaint().setStrokeWidth(10);
                map.getOverlays().add(pathOverlay);
            }

            totalDistanceMeters = 0.0;
            elapsedTime = 0L;
            lastLocation = null;
            startTime = System.currentTimeMillis();

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

    // ---------- Location updates & pace ----------

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

        // Buzz only when TOO SLOW, every few seconds
        if ("Too slow".equals(statusText)) {
            long now = System.currentTimeMillis();
            if (now - lastBuzzTime >= BUZZ_COOLDOWN_MS) {
                buzz(150);
                lastBuzzTime = now;
            }
        }
    }

    private void buzz(int durationMs) {
        Context ctx = getContext();
        if (ctx == null) return;

        try {
            Vibrator vibrator;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                VibratorManager vm =
                        (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
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
                VibrationEffect effect =
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(durationMs);
            }

        } catch (SecurityException e) {
            Log.w(TAG, "Missing VIBRATE permission, skipping buzz", e);
        }
    }

    // ---------- Stop & save ----------

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        saveRunToFirestore();

        isUpdatingLocation = false;
        isPaused = false;

        elapsedTime = 0L;
        totalDistanceMeters = 0.0;
        lastLocation = null;

        buttonStartLocation.setText("Start Run");
        buttonStartLocation.setBackgroundColor(Color.GREEN);
        buttonPause.setVisibility(View.GONE);
        inputTargetPace.setEnabled(true);
        if (bottomControlPanel != null) {
            bottomControlPanel.setCardBackgroundColor(Color.WHITE);
        }

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (map != null && pathOverlay != null) {
            map.getOverlays().remove(pathOverlay);
            pathOverlay = null;
            map.invalidate();
        }

        resetUI();
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

    private void incrementUserScore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid())
                .update("score", FieldValue.increment(1));
    }

    // ---------- Pause / resume ----------

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

    // ---------- UI helpers & lifecycle ----------

    private void resetUI() {
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: Ready");
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
