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
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MyRun extends Fragment {

    private static final String TAG = "MyRun";
    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private static final double TARGET_SPEED_MPS = 0.8;
    private static final double SPEED_TOLERANCE_MPS = 0.3;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // REQUIRED for OSMDroid (fixes tiles not loading)
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        View view = inflater.inflate(R.layout.fragment_my_run, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // UI
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

        // Map setup - FIXED
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true); // Enable pinch-to-zoom
        map.getController().setZoom(18.0); // Better initial zoom
        map.getController().setCenter(new GeoPoint(34.2478, -118.4390)); // Mission Hills, CA default

        // Add OSMDroid GPS tracking overlay
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(myLocationOverlay);

        // Initial UI
        textLat.setText("Lat: -");
        textLng.setText("Lng: -");
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: -");
        textTimer.setText("Time: 00:00");

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

    private void startLocationUpdates() {
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
            pathOverlay.getPaint().setStrokeWidth(8);
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

                    textTimer.setText(String.format("Time: %02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            };
            timerHandler.post(timerRunnable);

        } catch (SecurityException ignored) {}
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isUpdatingLocation = false;
        isPaused = false;
        elapsedTime = 0L;

        buttonStartLocation.setText("Start Location");
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
        textSpeed.setText("Speed (m/s): -");
        textStatus.setText("Status: -");
        textTimer.setText("Time: 00:00");
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

        textLat.setText("Lat: " + lat);
        textLng.setText("Lng: " + lng);
        textSpeed.setText("Speed (m/s): " + speed);

        GeoPoint userLocation = new GeoPoint(lat, lng);

        // Center map on first location update
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
            double diff = speed - TARGET_SPEED_MPS;
            if (diff > SPEED_TOLERANCE_MPS) statusText = "Too fast";
            else if (diff < -SPEED_TOLERANCE_MPS) statusText = "Too slow";
            else statusText = "On pace";
        }
        textStatus.setText("Status: " + statusText);

        switch (statusText) {
            case "On pace":
                setPaceColor(0xFF2bc335);
                break;
            case "Not moving":
                setPaceColor(0xFF9E9E9E);
                break;
            default:
                setPaceColor(0xFFac2121);
                break;
        }

        Log.d(TAG, "updateUIWithLocation: lat=" + lat + " lng=" + lng +
                " speed=" + speed + " status=" + statusText);
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