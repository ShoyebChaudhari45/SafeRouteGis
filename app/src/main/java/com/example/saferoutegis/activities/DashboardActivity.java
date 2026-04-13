package com.example.saferoutegis.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.utils.Constants;
import com.example.saferoutegis.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Dashboard – the app's home screen after login.
 *
 * Features:
 *  - Gradient header with greeting + report stats
 *  - Feature cards: Map, Traffic, Routes, Accidents, Potholes, Construction, Directions
 *  - Emergency section: Emergency Call 112, SOS Alert, Share Location
 *  - Nearby Services: Police, Hospital, Gas Station (opens Google Maps)
 *  - Safety Tips card
 *  - Bottom navigation bar
 */
public class DashboardActivity extends AppCompatActivity {

    private SessionManager  session;
    private DatabaseHelper  db;
    private TextView        tvWelcome, tvAlertCount;
    private FusedLocationProviderClient fusedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session       = new SessionManager(this);
        db            = DatabaseHelper.getInstance(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        setupToolbar();
        bindViews();
        setupCards();
        setupEmergency();
        setupNearbyServices();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAlertCount();
        BottomNavigationView nav = findViewById(R.id.bottomNavDashboard);
        if (nav != null) nav.setSelectedItemId(R.id.nav_home);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarDashboard);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void bindViews() {
        tvWelcome    = findViewById(R.id.tvWelcomeName);
        tvAlertCount = findViewById(R.id.tvAlertCount);
        tvWelcome.setText(getString(R.string.welcome_user, session.getUserName()));
    }

    private void refreshAlertCount() {
        new Thread(() -> {
            int count = db.getAllReports().size();
            runOnUiThread(() -> {
                if (count > 0) {
                    tvAlertCount.setVisibility(View.VISIBLE);
                    tvAlertCount.setText(count + " active " + (count == 1 ? "alert" : "alerts") + " on map");
                } else {
                    tvAlertCount.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void setupCards() {
        setCardClick(R.id.cardLiveMap,        v -> open(MapActivity.class));
        setCardClick(R.id.cardTraffic,        v -> openMap(true));
        setCardClick(R.id.cardSafeRoutes,     v -> open(DirectionsActivity.class));
        setCardClick(R.id.cardAccidents,      v -> openAlerts("ACCIDENT"));
        setCardClick(R.id.cardPotholes,       v -> openAlerts("POTHOLE"));
        setCardClick(R.id.cardConstruction,   v -> openAlerts("CONSTRUCTION"));
        setCardClick(R.id.cardDirections,     v -> open(DirectionsActivity.class));
    }

    // ── Emergency Section ─────────────────────────────────────────────────

    private void setupEmergency() {
        setCardClick(R.id.cardEmergencyCall, v -> dialEmergency());
        setCardClick(R.id.cardSOS,           v -> triggerSOS());
        setCardClick(R.id.cardShareLocation, v -> shareCurrentLocation());
    }

    private void dialEmergency() {
        new AlertDialog.Builder(this)
                .setTitle("📞 Emergency Call")
                .setMessage("Are you sure you want to call emergency services (112)?")
                .setPositiveButton("Call 112", (d, w) -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + getString(R.string.emergency_number)));
                    startActivity(dialIntent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void triggerSOS() {
        vibratePhone();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sos_sent_title))
                .setMessage(getString(R.string.sos_sent_message))
                .setPositiveButton("Call 112", (d, w) -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + getString(R.string.emergency_number)));
                    startActivity(dialIntent);
                })
                .setNegativeButton("Close", null)
                .show();
        Toast.makeText(this, "🆘 SOS Alert activated!", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void shareCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 300);
            return;
        }

        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                String url = "https://maps.google.com/?q=" + lat + "," + lng;
                String msg = "📍 My current location:\n" + url
                        + "\n\nShared via Road Safety GIS";

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "My Location");
                share.putExtra(Intent.EXTRA_TEXT, msg);
                startActivity(Intent.createChooser(share, "Share location via"));
            } else {
                Toast.makeText(this, "Waiting for GPS…", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 100, 100, 100, 100, 100, 200, 300, 200, 300, 200, 300, 200, 100, 100, 100, 100, 100};
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    // ── Nearby Services ───────────────────────────────────────────────────

    private void setupNearbyServices() {
        setCardClick(R.id.cardNearbyPolice,   v -> searchNearby("police station"));
        setCardClick(R.id.cardNearbyHospital, v -> searchNearby("hospital"));
        setCardClick(R.id.cardNearbyFuel,     v -> searchNearby("petrol pump"));
    }

    /**
     * Opens Google Maps searching for a nearby service type.
     */
    private void searchNearby(String query) {
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query + " near me"));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/" + Uri.encode(query + " near me"))));
        }
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavDashboard);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_map) {
                open(MapActivity.class);
                return true;
            } else if (id == R.id.nav_report) {
                open(ReportIssueActivity.class);
                return true;
            } else if (id == R.id.nav_alerts) {
                open(AlertsActivity.class);
                return true;
            } else if (id == R.id.nav_profile) {
                open(ProfileActivity.class);
                return true;
            }
            return false;
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void setCardClick(int cardId, View.OnClickListener listener) {
        View card = findViewById(cardId);
        if (card != null) card.setOnClickListener(listener);
    }

    private void open(Class<?> activity) {
        startActivity(new Intent(this, activity));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void openMap(boolean showTraffic) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.EXTRA_SHOW_TRAFFIC, showTraffic);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void openAlerts(String type) {
        Intent intent = new Intent(this, AlertsActivity.class);
        intent.putExtra(AlertsActivity.EXTRA_FILTER_TYPE, type);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    // ── Options menu ─────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            open(ProfileActivity.class);
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    session.logout();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
