package com.example.saferoutegis.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.Report;
import com.example.saferoutegis.utils.Constants;
import com.example.saferoutegis.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

/**
 * Live Map screen.
 *
 * Features:
 *  • Shows user's current location
 *  • Loads all reports as colour-coded markers WITH circular zones
 *  • Live speed monitor (km/h) via GPS updates
 *  • Shake-to-report — shake the phone to quickly open the report screen
 *  • Share location button — share current GPS coords via any app
 *  • Toggleable traffic layer
 *  • FAB to report a new issue
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, SensorEventListener {

    public static final String EXTRA_SHOW_TRAFFIC = "showTraffic";

    private static final double CIRCLE_RADIUS_METERS = 150;
    private static final float  SHAKE_THRESHOLD      = 12.0f;
    private static final long   SHAKE_COOLDOWN_MS    = 2000;

    private GoogleMap                   googleMap;
    private FusedLocationProviderClient  fusedLocation;
    private DatabaseHelper              db;
    private SwitchMaterial              switchTraffic;
    private FloatingActionButton        fabReport, fabShare;
    private TextView                    tvSpeed;
    private boolean                     trafficEnabled = false;

    // Speed monitor
    private LocationCallback locationCallback;
    private Location         lastKnownLocation;

    // Shake detector
    private SensorManager sensorManager;
    private Sensor        accelerometer;
    private long          lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db            = DatabaseHelper.getInstance(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarMap);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.map_title);
        }

        // Traffic toggle
        switchTraffic = findViewById(R.id.switchTraffic);
        trafficEnabled = getIntent().getBooleanExtra(EXTRA_SHOW_TRAFFIC, false);
        switchTraffic.setChecked(trafficEnabled);
        switchTraffic.setOnCheckedChangeListener((btn, checked) -> {
            trafficEnabled = checked;
            if (googleMap != null) googleMap.setTrafficEnabled(checked);
        });

        // Speed display
        tvSpeed = findViewById(R.id.tvSpeed);

        // FAB – report a new issue
        fabReport = findViewById(R.id.fabReportIssue);
        fabReport.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportIssueActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        // FAB – share location
        fabShare = findViewById(R.id.fabShareLocation);
        fabShare.setOnClickListener(v -> shareCurrentLocation());

        // Shake detector setup
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Location callback for speed updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    lastKnownLocation = loc;
                    updateSpeed(loc);
                }
            }
        };

        // Initialise map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    // ── OnMapReadyCallback ────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.setTrafficEnabled(trafficEnabled);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.REQUEST_LOCATION_PERMISSION);
        }

        loadReportMarkers();
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        googleMap.setMyLocationEnabled(true);

        // Get initial location
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lastKnownLocation = location;
                LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, Constants.DEFAULT_ZOOM));
                checkNearbyHazards(location);
            }
        });

        // Start continuous location updates for speed
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();
        fusedLocation.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    // ── Speed Monitor ─────────────────────────────────────────────────────────

    private void updateSpeed(Location location) {
        if (location.hasSpeed()) {
            float speedKmh = location.getSpeed() * 3.6f; // m/s to km/h
            tvSpeed.setText(String.valueOf((int) speedKmh));

            // Change color if over 80 km/h (overspeed warning)
            if (speedKmh > 80) {
                tvSpeed.setTextColor(Color.parseColor("#E53935")); // Red
            } else if (speedKmh > 60) {
                tvSpeed.setTextColor(Color.parseColor("#F57C00")); // Orange
            } else {
                tvSpeed.setTextColor(Color.parseColor("#212121")); // Normal dark
            }
        } else {
            tvSpeed.setText("0");
            tvSpeed.setTextColor(Color.parseColor("#212121"));
        }
    }

    // ── Share Location ────────────────────────────────────────────────────────

    private void shareCurrentLocation() {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = lastKnownLocation.getLatitude();
        double lng = lastKnownLocation.getLongitude();
        String mapsUrl = "https://maps.google.com/?q=" + lat + "," + lng;
        String message = "📍 My current location:\n" + mapsUrl
                + "\n\nShared via Road Safety GIS";

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "My Location — Road Safety GIS");
        share.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(share, "Share location via"));
    }

    // ── Shake to Report ───────────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z)
                             - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now;
                onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }

    private void onShakeDetected() {
        // Vibrate briefly
        NotificationHelper.vibrateDevice(this);

        Toast.makeText(this, "📳 Shake detected — opening quick report!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, ReportIssueActivity.class));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    // ── Map markers + circles ─────────────────────────────────────────────────

    private void loadReportMarkers() {
        new Thread(() -> {
            List<Report> reports = db.getAllReports();
            runOnUiThread(() -> {
                googleMap.clear();
                for (Report r : reports) {
                    LatLng pos = new LatLng(r.getLatitude(), r.getLongitude());

                    MarkerOptions markerOpts = new MarkerOptions()
                            .position(pos)
                            .title(r.getTitle())
                            .snippet(r.getTypeLabel() + " • " + r.getSeverityLabel()
                                     + "\n" + r.getTimestamp())
                            .icon(BitmapDescriptorFactory.defaultMarker(markerHue(r.getType())));
                    googleMap.addMarker(markerOpts);

                    int baseColor = circleColor(r.getType());
                    int fillColor  = Color.argb(50, Color.red(baseColor),
                                                    Color.green(baseColor),
                                                    Color.blue(baseColor));
                    int strokeColor = Color.argb(150, Color.red(baseColor),
                                                     Color.green(baseColor),
                                                     Color.blue(baseColor));

                    double radius = CIRCLE_RADIUS_METERS;
                    if (r.getSeverity() == Report.SEVERITY_MEDIUM) radius = 200;
                    if (r.getSeverity() == Report.SEVERITY_HIGH)   radius = 300;

                    googleMap.addCircle(new CircleOptions()
                            .center(pos).radius(radius)
                            .fillColor(fillColor).strokeColor(strokeColor)
                            .strokeWidth(2f));
                }
            });
        }).start();
    }

    private void checkNearbyHazards(Location location) {
        new Thread(() -> {
            List<Report> nearby = db.getNearbyReports(
                    location.getLatitude(), location.getLongitude(), Constants.ALERT_RADIUS_KM);
            if (!nearby.isEmpty()) {
                String msg = nearby.size() + " road hazard(s) within 2 km of your location!";
                runOnUiThread(() ->
                        NotificationHelper.showNearbyHazard(MapActivity.this, msg));
            }
        }).start();
    }

    private float markerHue(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_VIOLET;
        switch (type) {
            case Report.TYPE_ACCIDENT:     return BitmapDescriptorFactory.HUE_RED;
            case Report.TYPE_POTHOLE:      return BitmapDescriptorFactory.HUE_ORANGE;
            case Report.TYPE_CONSTRUCTION: return BitmapDescriptorFactory.HUE_YELLOW;
            case Report.TYPE_TRAFFIC:      return BitmapDescriptorFactory.HUE_AZURE;
            default:                       return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }

    private int circleColor(String type) {
        if (type == null) return Color.GRAY;
        switch (type) {
            case Report.TYPE_ACCIDENT:     return Color.parseColor("#E53935");
            case Report.TYPE_POTHOLE:      return Color.parseColor("#F57C00");
            case Report.TYPE_CONSTRUCTION: return Color.parseColor("#FBC02D");
            case Report.TYPE_TRAFFIC:      return Color.parseColor("#1E88E5");
            default:                       return Color.GRAY;
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        if (googleMap != null) loadReportMarkers();
        // Register shake sensor
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister shake sensor
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Stop location updates to save battery
        fusedLocation.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, getString(R.string.error_location), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
