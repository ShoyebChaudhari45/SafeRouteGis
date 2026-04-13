package com.example.saferoutegis.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.User;
import com.example.saferoutegis.utils.PasswordUtils;
import com.example.saferoutegis.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login screen – validates credentials against the SQLite database,
 * creates a session, and routes the user to the Dashboard.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout    tilEmail, tilPassword;
    private TextInputEditText  etEmail, etPassword;
    private MaterialButton     btnLogin;
    private TextView           tvGoToRegister;
    private LinearProgressIndicator progressBar;

    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        // If already logged in, skip to Dashboard
        if (session.isLoggedIn()) {
            goToDashboard();
            return;
        }

        bindViews();
        setListeners();
    }

    private void bindViews() {
        tilEmail       = findViewById(R.id.tilLoginEmail);
        tilPassword    = findViewById(R.id.tilLoginPassword);
        etEmail        = findViewById(R.id.etLoginEmail);
        etPassword     = findViewById(R.id.etLoginPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar    = findViewById(R.id.progressLogin);
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void attemptLogin() {
        // Reset errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_fields));
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_fields));
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        // Run DB lookup on a background thread to avoid blocking the UI
        new Thread(() -> {
            User user = db.getUserByEmail(email);
            runOnUiThread(() -> {
                showLoading(false);
                if (user == null || !PasswordUtils.verifyPassword(password, user.getPassword())) {
                    tilPassword.setError(getString(R.string.error_invalid_credentials));
                } else {
                    // Success – create session and navigate
                    session.createLoginSession(user.getId(), user.getEmail(), user.getName());
                    goToDashboard();
                }
            });
        }).start();
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
}
