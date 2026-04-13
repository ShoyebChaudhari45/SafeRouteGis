package com.example.saferoutegis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.saferoutegis.activities.DashboardActivity;
import com.example.saferoutegis.activities.LoginActivity;
import com.example.saferoutegis.utils.NotificationHelper;
import com.example.saferoutegis.utils.SessionManager;

/**
 * SplashActivity – the app's launcher screen.
 *
 * Shows the brand gradient, animates the logo with a pulsing glow,
 * then routes to:
 *   → DashboardActivity  (if a user session exists)
 *   → LoginActivity      (otherwise)
 */
@SuppressLint("CustomSplashScreen")
public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the notification channel once at startup
        NotificationHelper.createNotificationChannel(this);

        // Animate elements
        ImageView ivLogo     = findViewById(R.id.ivSplashLogo);
        View viewGlow        = findViewById(R.id.viewLogoGlow);
        TextView tvAppName   = findViewById(R.id.tvSplashAppName);
        TextView tvTagline   = findViewById(R.id.tvSplashTagline);
        LinearLayout layoutFeatures = findViewById(R.id.layoutFeatures);

        // Logo: scale + bounce in
        AnimationSet logoAnim = buildBounceIn(0);
        ivLogo.startAnimation(logoAnim);

        // Glow: pulse in
        AnimationSet glowAnim = buildFadeScaleIn(200);
        viewGlow.startAnimation(glowAnim);

        // App name: fade + slide
        AnimationSet nameAnim = buildFadeScaleIn(400);
        tvAppName.startAnimation(nameAnim);

        // Tagline: fade in
        AnimationSet tagAnim = buildFadeScaleIn(700);
        tvTagline.startAnimation(tagAnim);

        // Feature row: fade in
        if (layoutFeatures != null) {
            AnimationSet featAnim = buildFadeScaleIn(1000);
            layoutFeatures.startAnimation(featAnim);
        }

        // Navigate after the delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        SessionManager session = new SessionManager(this);
        Class<?> target = session.isLoggedIn() ? DashboardActivity.class : LoginActivity.class;
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // Logo bounce animation (scale from small to slightly over-sized, then settle)
    private AnimationSet buildBounceIn(long startOffset) {
        ScaleAnimation scale = new ScaleAnimation(
                0.3f, 1f, 0.3f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(900);
        scale.setStartOffset(startOffset);
        scale.setInterpolator(new OvershootInterpolator(1.5f));

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(600);
        fade.setStartOffset(startOffset);

        AnimationSet set = new AnimationSet(false);
        set.addAnimation(scale);
        set.addAnimation(fade);
        set.setFillAfter(true);
        return set;
    }

    // Standard fade + scale-up animation helper
    private AnimationSet buildFadeScaleIn(long startOffset) {
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(700);
        fade.setStartOffset(startOffset);

        ScaleAnimation scale = new ScaleAnimation(
                0.7f, 1f, 0.7f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(700);
        scale.setStartOffset(startOffset);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(fade);
        set.addAnimation(scale);
        set.setFillAfter(true);
        return set;
    }
}
