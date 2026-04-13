package com.example.saferoutegis.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.Report;
import com.example.saferoutegis.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Directions screen.
 *
 * Fetches routes from the Google Directions API, displays them on the map,
 * and – when "Safest Route" is selected – picks the route with fewest
 * nearby road hazards (from the local SQLite database).
 *
 * Falls back to opening Google Maps app if the API call fails.
 */
public class DirectionsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextInputLayout   tilOrigin, tilDestination;
    private TextInputEditText etOrigin, etDestination;
    private RadioGroup        rgRouteType;
    private MaterialButton    btnGetDirections;
    private TextView          tvDistance, tvDuration, tvHazards;
    private LinearProgressIndicator progressBar;

    private GoogleMap      googleMap;
    private DatabaseHelper db;
    private OkHttpClient   httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        db         = DatabaseHelper.getInstance(this);
        httpClient = new OkHttpClient();

        Toolbar toolbar = findViewById(R.id.toolbarDirections);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.directions_title);
        }

        bindViews();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDirections);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void bindViews() {
        tilOrigin        = findViewById(R.id.tilOrigin);
        tilDestination   = findViewById(R.id.tilDestination);
        etOrigin         = findViewById(R.id.etOrigin);
        etDestination    = findViewById(R.id.etDestination);
        rgRouteType      = findViewById(R.id.rgRouteType);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        tvDistance       = findViewById(R.id.tvDirectionDistance);
        tvDuration       = findViewById(R.id.tvDirectionDuration);
        tvHazards        = findViewById(R.id.tvDirectionHazards);
        progressBar      = findViewById(R.id.progressDirections);

        btnGetDirections.setOnClickListener(v -> fetchDirections());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    // ── Fetch directions from Google API ──────────────────────────────────────

    private void fetchDirections() {
        tilOrigin.setError(null);
        tilDestination.setError(null);

        String origin = etOrigin.getText() != null ? etOrigin.getText().toString().trim() : "";
        String dest   = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";

        if (TextUtils.isEmpty(origin)) {
            tilOrigin.setError(getString(R.string.error_empty_fields)); return;
        }
        if (TextUtils.isEmpty(dest)) {
            tilDestination.setError(getString(R.string.error_empty_fields)); return;
        }

        // Read the API key from string resources (set in strings.xml)
        String apiKey = getString(R.string.google_maps_key);
        if (apiKey.equals("YOUR_GOOGLE_MAPS_API_KEY") || apiKey.isEmpty()) {
            // No API key set — fall back to Google Maps app
            openGoogleMapsApp(origin, dest);
            return;
        }

        boolean wantSafeRoute = rgRouteType.getCheckedRadioButtonId() == R.id.rbSafeRoute;
        showLoading(true);

        new Thread(() -> {
            try {
                String url = Constants.DIRECTIONS_BASE_URL
                        + "?origin="      + URLEncoder.encode(origin, "UTF-8")
                        + "&destination=" + URLEncoder.encode(dest, "UTF-8")
                        + "&alternatives=true"
                        + "&key="         + apiKey;

                Request  req  = new Request.Builder().url(url).build();
                Response resp = httpClient.newCall(req).execute();

                if (resp.body() == null) throw new IOException("Empty response");
                String body = resp.body().string();
                parseAndDisplay(body, wantSafeRoute, origin, dest);

            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // Fallback: open in Google Maps app
                    Toast.makeText(this, "Opening directions in Google Maps...",
                            Toast.LENGTH_SHORT).show();
                    openGoogleMapsApp(origin, dest);
                });
            }
        }).start();
    }

    private void parseAndDisplay(String json, boolean pickSafest, String origin, String dest) {
        try {
            JSONObject root   = new JSONObject(json);
            String     status = root.getString("status");

            if (!"OK".equals(status)) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if ("REQUEST_DENIED".equals(status) || "OVER_QUERY_LIMIT".equals(status)) {
                        // API key issue — fallback to Google Maps app
                        Toast.makeText(this,
                                "Opening directions in Google Maps app...",
                                Toast.LENGTH_SHORT).show();
                        openGoogleMapsApp(origin, dest);
                    } else if ("ZERO_RESULTS".equals(status)) {
                        Toast.makeText(this, "No route found between these locations.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, getString(R.string.error_directions)
                                + " (status: " + status + ")", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }

            JSONArray routes = root.getJSONArray("routes");
            if (routes.length() == 0) {
                runOnUiThread(() -> { showLoading(false);
                    Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show(); });
                return;
            }

            // Get all local reports once for hazard counting
            List<Report> allReports = db.getAllReports();

            // Pick the route index (0 = fastest, or safest if requested)
            int chosenIdx = 0;
            if (pickSafest && routes.length() > 1) {
                chosenIdx = safestRouteIndex(routes, allReports);
            }

            JSONObject chosenRoute = routes.getJSONObject(chosenIdx);
            JSONObject leg         = chosenRoute.getJSONArray("legs").getJSONObject(0);
            String     distance    = leg.getJSONObject("distance").getString("text");
            String     duration    = leg.getJSONObject("duration").getString("text");
            String     polyline    = chosenRoute.getJSONObject("overview_polyline").getString("points");

            List<LatLng> points   = PolyUtil.decode(polyline);
            int          hazards  = countHazards(points, allReports);

            runOnUiThread(() -> {
                showLoading(false);
                drawRoute(points, pickSafest);
                tvDistance.setText(getString(R.string.distance_label, distance));
                tvDuration.setText(getString(R.string.duration_label, duration));
                tvHazards.setText(getString(R.string.hazards_on_route, hazards));
                tvDistance.setVisibility(View.VISIBLE);
                tvDuration.setVisibility(View.VISIBLE);
                tvHazards.setVisibility(View.VISIBLE);

                // Add start / end markers
                LatLng start = points.get(0);
                LatLng end   = points.get(points.size() - 1);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(start).title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                googleMap.addMarker(new MarkerOptions().position(end).title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // Draw route again after clear
                drawRoute(points, pickSafest);

                // Fit camera to route
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                for (LatLng p : points) boundsBuilder.include(p);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
            });

        } catch (JSONException e) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, "Error parsing directions response.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Open Google Maps app for turn-by-turn directions.
     * This always works regardless of API key restrictions.
     */
    private void openGoogleMapsApp(String origin, String dest) {
        try {
            String uri = "https://www.google.com/maps/dir/"
                    + URLEncoder.encode(origin, "UTF-8") + "/"
                    + URLEncoder.encode(dest, "UTF-8");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // If Google Maps app is not installed, open in browser
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open maps.", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(List<LatLng> points, boolean isSafe) {
        PolylineOptions opts = new PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(isSafe ? 0xFF43A047 : 0xFF1E88E5); // green = safe, blue = fastest
        googleMap.addPolyline(opts);
    }

    /**
     * Return the index of the route with the fewest nearby hazards.
     */
    private int safestRouteIndex(JSONArray routes, List<Report> reports) throws JSONException {
        int bestIdx   = 0;
        int minHazards = Integer.MAX_VALUE;
        for (int i = 0; i < routes.length(); i++) {
            String polyline = routes.getJSONObject(i)
                    .getJSONObject("overview_polyline").getString("points");
            List<LatLng> pts    = PolyUtil.decode(polyline);
            int          count  = countHazards(pts, reports);
            if (count < minHazards) {
                minHazards = count;
                bestIdx    = i;
            }
        }
        return bestIdx;
    }

    /**
     * Count how many reports lie within NEARBY_RADIUS_KM of any point on a route.
     */
    private int countHazards(List<LatLng> routePoints, List<Report> reports) {
        int count = 0;
        for (Report r : reports) {
            LatLng rLatLng = new LatLng(r.getLatitude(), r.getLongitude());
            if (PolyUtil.isLocationOnPath(rLatLng, routePoints, true,
                    Constants.NEARBY_RADIUS_KM * 1000)) { // PolyUtil uses metres
                count++;
            }
        }
        return count;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGetDirections.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
