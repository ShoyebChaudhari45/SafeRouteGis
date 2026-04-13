package com.example.saferoutegis.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.saferoutegis.R;
import com.example.saferoutegis.activities.AlertsActivity;

/**
 * Builds and shows rich, visually distinct local notifications for road
 * safety events. Two separate channels are used so users can tune
 * hazard alerts independently from report confirmations.
 */
public final class NotificationHelper {

    // ── Notification IDs ──────────────────────────────────────────────────────
    private static final int NOTIF_ID_REPORT = 1001;
    private static final int NOTIF_ID_NEARBY = 1002;

    // ── Channel IDs ───────────────────────────────────────────────────────────
    private static final String CHANNEL_HAZARD  = "channel_hazard_alerts";
    private static final String CHANNEL_CONFIRM = "channel_report_confirm";

    // ── Vibration patterns ────────────────────────────────────────────────────
    /** Urgent: short buzz → pause → two longer pulses */
    private static final long[] VIBRATE_HAZARD  = {0, 150, 80, 300, 80, 300};
    /** Gentle: single short buzz for confirmations */
    private static final long[] VIBRATE_CONFIRM = {0, 120};

    private NotificationHelper() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  Channel registration  (call once at app start, safe to repeat)
    // ══════════════════════════════════════════════════════════════════════════

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // Channel 1 — Nearby hazard alerts  (HIGH importance → heads-up banner)
        NotificationChannel hazardChannel = new NotificationChannel(
                CHANNEL_HAZARD,
                "Road Hazard Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        hazardChannel.setDescription("Heads-up alerts for road hazards near your location");
        hazardChannel.enableVibration(true);
        hazardChannel.setVibrationPattern(VIBRATE_HAZARD);
        hazardChannel.enableLights(true);
        hazardChannel.setLightColor(0xFFE53935);
        hazardChannel.setSound(defaultSound, audio);
        hazardChannel.setShowBadge(true);
        nm.createNotificationChannel(hazardChannel);

        // Channel 2 — Report confirmation  (DEFAULT importance → quiet banner)
        NotificationChannel confirmChannel = new NotificationChannel(
                CHANNEL_CONFIRM,
                "Report Confirmations",
                NotificationManager.IMPORTANCE_DEFAULT);
        confirmChannel.setDescription("Confirmation when your road hazard report is saved");
        confirmChannel.enableVibration(true);
        confirmChannel.setVibrationPattern(VIBRATE_CONFIRM);
        confirmChannel.setShowBadge(true);
        nm.createNotificationChannel(confirmChannel);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Notify the user that their report was saved successfully.
     *
     * @param type  Human-readable type label, e.g. "Accident"
     * @param title The report title entered by the user
     */
    public static void showReportSubmitted(Context context, String type, String title) {
        if (!hasPermission(context)) return;
        vibrateDevice(context, VIBRATE_CONFIRM);

        // "View Alerts" action → opens Alerts screen
        PendingIntent viewIntent = buildAlertsIntent(context);

        Bitmap largeIcon = buildLargeIcon(context, R.drawable.ic_nav_alerts, 0xFF43A047);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CONFIRM)
                .setSmallIcon(R.drawable.ic_nav_alerts)
                .setLargeIcon(largeIcon)
                .setColor(0xFF43A047)                      // green accent
                .setContentTitle("Report submitted")
                .setContentText(type + ": " + title)
                .setSubText("SafeRoute GIS")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your " + type.toLowerCase() + " report \"" + title
                                + "\" has been saved and is now visible to other drivers on the map.")
                        .setSummaryText(type))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(VIBRATE_CONFIRM)
                .setAutoCancel(true)
                .setContentIntent(viewIntent)
                .addAction(R.drawable.ic_nav_alerts, "View Alerts", viewIntent);

        getManager(context).notify(NOTIF_ID_REPORT, builder.build());
    }

    /**
     * Notify the user about a nearby road hazard with a high-priority heads-up banner.
     *
     * @param message Full hazard description
     */
    public static void showNearbyHazard(Context context, String message) {
        if (!hasPermission(context)) return;
        vibrateDevice(context, VIBRATE_HAZARD);

        PendingIntent viewIntent  = buildAlertsIntent(context);
        PendingIntent dismissIntent = buildDismissIntent(context);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Bitmap largeIcon = buildLargeIcon(context, R.drawable.ic_feature_accident, 0xFFD32F2F);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_HAZARD)
                .setSmallIcon(R.drawable.ic_nav_alerts)
                .setLargeIcon(largeIcon)
                .setColor(0xFFD32F2F)                      // red accent
                .setColorized(true)
                .setContentTitle("Nearby Road Hazard")
                .setContentText(message)
                .setSubText("SafeRoute GIS  •  Tap to view")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setSummaryText("Road safety alert"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(VIBRATE_HAZARD)
                .setSound(alarmSound)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setContentIntent(viewIntent)
                .addAction(R.drawable.ic_nav_map,    "View on Map",  viewIntent)
                .addAction(R.drawable.ic_nav_alerts, "All Alerts",   viewIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissIntent);

        getManager(context).notify(NOTIF_ID_NEARBY, builder.build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Vibration helper
    // ══════════════════════════════════════════════════════════════════════════

    public static void vibrateDevice(Context context, long[] pattern) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    /** Convenience overload using the default hazard pattern. */
    public static void vibrateDevice(Context context) {
        vibrateDevice(context, VIBRATE_HAZARD);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Build a circular large-icon Bitmap: a solid coloured disc with the
     * given vector icon centred and tinted white inside it.
     */
    private static Bitmap buildLargeIcon(Context context, int iconRes, int circleColor) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = (int) (48 * density);

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Coloured circle
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(circleColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // White icon centred
        Drawable icon = ContextCompat.getDrawable(context, iconRes);
        if (icon != null) {
            int pad = size / 5;
            Drawable wrapped = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(wrapped, Color.WHITE);
            wrapped.setBounds(pad, pad, size - pad, size - pad);
            wrapped.draw(canvas);
        }

        return bmp;
    }

    /** PendingIntent that opens the Alerts screen. */
    private static PendingIntent buildAlertsIntent(Context context) {
        Intent intent = new Intent(context, AlertsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** PendingIntent that simply cancels the notification (no-op broadcast). */
    private static PendingIntent buildDismissIntent(Context context) {
        Intent intent = new Intent("com.example.saferoutegis.DISMISS_NOTIFICATION");
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private static NotificationManager getManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
