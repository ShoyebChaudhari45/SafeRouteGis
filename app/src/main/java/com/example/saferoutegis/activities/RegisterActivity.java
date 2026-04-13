package com.example.saferoutegis.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Registration screen – collects user details, hashes the password,
 * stores the account in SQLite, and auto-logs the user in.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout   tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialButton    btnRegister;
    private TextView          tvGoToLogin;
    private LinearProgressIndicator progressBar;

    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db      = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);

        bindViews();
        setListeners();
    }

    private void bindViews() {
        tilName            = findViewById(R.id.tilRegisterName);
        tilEmail           = findViewById(R.id.tilRegisterEmail);
        tilPhone           = findViewById(R.id.tilRegisterPhone);
        tilPassword        = findViewById(R.id.tilRegisterPassword);
        tilConfirmPassword = findViewById(R.id.tilRegisterConfirmPassword);

        etName            = findViewById(R.id.etRegisterName);
        etEmail           = findViewById(R.id.etRegisterEmail);
        etPhone           = findViewById(R.id.etRegisterPhone);
        etPassword        = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);

        btnRegister  = findViewById(R.id.btnRegister);
        tvGoToLogin  = findViewById(R.id.tvGoToLogin);
        progressBar  = findViewById(R.id.progressRegister);
    }

    private void setListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvGoToLogin.setOnClickListener(v -> {
            finish(); // go back to LoginActivity
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void attemptRegister() {
        // Reset all errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name    = text(etName);
        String email   = text(etEmail);
        String phone   = text(etPhone);
        String pass    = text(etPassword);
        String confirm = text(etConfirmPassword);

        // Validation chain
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_empty_fields)); return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_fields)); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email)); return;
        }
        if (TextUtils.isEmpty(pass)) {
            tilPassword.setError(getString(R.string.error_empty_fields)); return;
        }
        if (pass.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_length)); return;
        }
        if (!pass.equals(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match)); return;
        }

        showLoading(true);

        final String finalName  = name;
        final String finalEmail = email;
        final String finalPhone = phone;
        final String finalPass  = pass;

        new Thread(() -> {
            if (db.isEmailRegistered(finalEmail)) {
                runOnUiThread(() -> {
                    showLoading(false);
                    tilEmail.setError(getString(R.string.error_email_taken));
                });
                return;
            }

            // Create and insert user
            String timestamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            User user = new User(finalName, finalEmail,
                    PasswordUtils.hashPassword(finalPass), finalPhone, timestamp);
            long rowId = db.insertUser(user);

            runOnUiThread(() -> {
                showLoading(false);
                if (rowId > 0) {
                    // Auto-login the new user
                    session.createLoginSession((int) rowId, finalEmail, finalName);
                    goToDashboard();
                } else {
                    tilEmail.setError("Registration failed. Please try again.");
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

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}
