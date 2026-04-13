package com.example.saferoutegis.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.User;
import com.example.saferoutegis.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Profile screen.
 *
 * Displays the logged-in user's details and statistics.
 * Allows inline editing of name and phone number.
 * Includes preferences: Dark Mode toggle, Notification toggle.
 * Provides a logout button and app info section.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_SETTINGS = "app_settings";
    private static final String KEY_DARK_MODE  = "dark_mode";
    private static final String KEY_NOTIF_ON   = "notifications_enabled";

    private TextView   tvProfileAvatar, tvProfileName, tvProfileEmail, tvProfilePhone;
    private TextView   tvReportCount, tvMemberSince;
    private TextInputLayout   tilEditName, tilEditPhone;
    private TextInputEditText etEditName, etEditPhone;
    private MaterialButton    btnSave, btnLogout;
    private View              editContainer;
    private SwitchMaterial    switchDarkMode, switchNotifications;

    private DatabaseHelper db;
    private SessionManager session;
    private User           currentUser;
    private boolean        editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile_title);
        }

        bindViews();
        loadUserData();
        setupPreferences();
        setupAppInfo();
    }

    private void bindViews() {
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        tvProfileName  = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvReportCount  = findViewById(R.id.tvProfileReportCount);
        tvMemberSince  = findViewById(R.id.tvProfileMemberSince);

        tilEditName    = findViewById(R.id.tilProfileEditName);
        tilEditPhone   = findViewById(R.id.tilProfileEditPhone);
        etEditName     = findViewById(R.id.etProfileEditName);
        etEditPhone    = findViewById(R.id.etProfileEditPhone);

        editContainer  = findViewById(R.id.layoutEditProfile);
        btnSave        = findViewById(R.id.btnProfileSave);
        btnLogout      = findViewById(R.id.btnProfileLogout);

        switchDarkMode     = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);

        MaterialButton btnEdit = findViewById(R.id.btnProfileEdit);
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v  -> saveProfile());
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadUserData() {
        new Thread(() -> {
            currentUser = db.getUserById(session.getUserId());
            if (currentUser == null) return;
            int reportCount = db.getReportCountByUserId(currentUser.getId());

            runOnUiThread(() -> {
                tvProfileName.setText(currentUser.getName());
                tvProfileEmail.setText(currentUser.getEmail());

                // Set avatar initial from user name
                String userName = currentUser.getName();
                if (userName != null && !userName.isEmpty()) {
                    tvProfileAvatar.setText(String.valueOf(userName.charAt(0)).toUpperCase());
                } else {
                    tvProfileAvatar.setText("U");
                }
                tvProfilePhone.setText(currentUser.getPhone() != null
                        && !currentUser.getPhone().isEmpty()
                        ? currentUser.getPhone() : "Not provided");
                tvReportCount.setText(String.valueOf(reportCount));
                tvMemberSince.setText(getString(R.string.member_since,
                        currentUser.getCreatedAt() != null ? currentUser.getCreatedAt() : "Unknown"));

                // Pre-fill edit fields
                etEditName.setText(currentUser.getName());
                etEditPhone.setText(currentUser.getPhone());
            });
        }).start();
    }

    // ── Preferences ───────────────────────────────────────────────────────

    private void setupPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);

        // Dark mode
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Notifications
        boolean notifOn = prefs.getBoolean(KEY_NOTIF_ON, true);
        switchNotifications.setChecked(notifOn);
        switchNotifications.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_NOTIF_ON, checked).apply();
            Toast.makeText(this,
                    checked ? "🔔 Notifications enabled" : "🔕 Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ── App Info ───────────────────────────────────────────────────────────

    private void setupAppInfo() {
        LinearLayout rateApp = findViewById(R.id.layoutRateApp);
        if (rateApp != null) {
            rateApp.setOnClickListener(v -> {
                // Try to open Play Store, fallback to browser
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            });
        }
    }

    // ── Edit Profile ──────────────────────────────────────────────────────

    private void toggleEditMode() {
        editMode = !editMode;
        editContainer.setVisibility(editMode ? View.VISIBLE : View.GONE);
    }

    private void saveProfile() {
        tilEditName.setError(null);
        String name  = etEditName.getText()  != null ? etEditName.getText().toString().trim()  : "";
        String phone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            tilEditName.setError(getString(R.string.error_empty_fields));
            return;
        }

        currentUser.setName(name);
        currentUser.setPhone(phone);

        new Thread(() -> {
            int rows = db.updateUser(currentUser);
            runOnUiThread(() -> {
                if (rows > 0) {
                    session.updateUserName(name);
                    tvProfileName.setText(name);
                    tvProfilePhone.setText(phone.isEmpty() ? "Not provided" : phone);
                    // Update avatar
                    tvProfileAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    toggleEditMode();
                    Toast.makeText(this, "✅ Profile updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void confirmLogout() {
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
