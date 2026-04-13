package com.example.saferoutegis.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.saferoutegis.R;
import com.example.saferoutegis.activities.AlertsActivity;

/**
 * Helper class for showing local notifications about road safety events.
 * Notifications include vibration and sound to alert users immediately.
 */
public final class NotificationHelper {

    private static final int NOTIF_ID_REPORT    = 1001;
    private static final int NOTIF_ID_NEARBY    = 1002;

    // Vibration pattern: short buzz, pause, long buzz
    private static final long[] VIBRATE_PATTERN = {0, 200, 100, 400};

    private NotificationHelper() {}

    /**
     * Create the notification channel (must be called once at app start, safe to call repeatedly).
     * Required on Android 8.0 (API 26) and above.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH  // HIGH for heads-up + vibration
            );
            channel.setDescription("Alerts for road hazards near your location");
            channel.enableVibration(true);
            channel.setVibrationPattern(VIBRATE_PATTERN);
            channel.enableLights(true);
            channel.setLightColor(0xFFE53935); // Red light

            // Sound
            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(defaultSound, audioAttr);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show a notification confirming that a report was submitted successfully.
     * Includes vibration feedback.
     */
    public static void showReportSubmitted(Context context, String type, String title) {
        if (!hasNotificationPermission(context)) return;

        // Vibrate the device
        vibrateDevice(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_feature_accident)
                .setContentTitle("✅ Report Submitted")
                .setContentText(type + " reported: " + title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your " + type + " report \"" + title
                                + "\" has been saved and is now visible on the map."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(VIBRATE_PATTERN)
                .setAutoCancel(true);

        getManager(context).notify(NOTIF_ID_REPORT, builder.build());
    }

    /**
     * Show a notification about a nearby road hazard.
     * Includes vibration and heads-up notification.
     */
    public static void showNearbyHazard(Context context, String message) {
        if (!hasNotificationPermission(context)) return;

        // Vibrate with urgency
        vibrateDevice(context);

        // Tapping the notification opens the Alerts screen
        Intent intent = new Intent(context, AlertsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_feature_accident)
                .setContentTitle("⚠️ Nearby Road Hazard!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(VIBRATE_PATTERN)
                .setSound(alarmSound)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        getManager(context).notify(NOTIF_ID_NEARBY, builder.build());
    }

    // ── Vibration helper ────────────────────────────────────────────────

    /**
     * Vibrate the device with the standard alert pattern.
     */
    public static void vibrateDevice(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, -1));
        } else {
            vibrator.vibrate(VIBRATE_PATTERN, -1);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permission not required below API 33
    }

    private static NotificationManager getManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
