package com.example.saferoutegis.utils;

/**
 * App-wide constants.
 */
public final class Constants {

    private Constants() {} // prevent instantiation

    // ── Google Maps ───────────────────────────────────────────────────────────
    // NOTE: Replace with your actual key from https://console.cloud.google.com/
    // Enable: Maps SDK for Android  +  Directions API
    public static final String MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY";

    // Google Directions REST endpoint
    public static final String DIRECTIONS_BASE_URL =
            "https://maps.googleapis.com/maps/api/directions/json";

    // ── Permission Request Codes ──────────────────────────────────────────────
    public static final int REQUEST_LOCATION_PERMISSION  = 100;
    public static final int REQUEST_STORAGE_PERMISSION   = 101;
    public static final int REQUEST_CAMERA_PERMISSION    = 102;
    public static final int REQUEST_NOTIFICATION_PERM    = 103;

    // ── Activity Result Codes ─────────────────────────────────────────────────
    public static final int PICK_IMAGE_REQUEST = 200;

    // ── Notification ─────────────────────────────────────────────────────────
    public static final String NOTIFICATION_CHANNEL_ID   = "SafeRouteAlerts";
    public static final String NOTIFICATION_CHANNEL_NAME = "Road Safety Alerts";

    // ── SharedPreferences ─────────────────────────────────────────────────────
    public static final String PREFS_NAME          = "SafeRoutePrefs";
    public static final String KEY_IS_LOGGED_IN    = "isLoggedIn";
    public static final String KEY_USER_ID         = "userId";
    public static final String KEY_USER_EMAIL      = "userEmail";
    public static final String KEY_USER_NAME       = "userName";

    // ── Safe-Route hazard search radius (km) ─────────────────────────────────
    public static final double NEARBY_RADIUS_KM = 0.5;   // 500 m from route
    public static final double ALERT_RADIUS_KM  = 2.0;   // 2 km for nearby alerts

    // ── Map defaults ─────────────────────────────────────────────────────────
    public static final float DEFAULT_ZOOM = 15f;

    // ── Image storage folder (inside app's filesDir) ─────────────────────────
    public static final String IMAGE_DIR = "report_images";
}
