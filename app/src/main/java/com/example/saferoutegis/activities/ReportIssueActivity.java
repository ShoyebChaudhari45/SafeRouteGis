package com.example.saferoutegis.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.saferoutegis.R;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.Report;
import com.example.saferoutegis.utils.Constants;
import com.example.saferoutegis.utils.NotificationHelper;
import com.example.saferoutegis.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Report Issue screen.
 *
 * Lets the user report an accident, pothole, construction zone, or traffic jam.
 * Auto-detects GPS location, optional photo upload, severity selector.
 * Report is saved to SQLite and a local notification is fired.
 */
public class ReportIssueActivity extends AppCompatActivity {

    private TextInputLayout   tilTitle, tilDescription;
    private TextInputEditText etTitle, etDescription;
    private RadioGroup        rgType, rgSeverity;
    private TextView          tvLocationStatus;
    private ImageView         ivImagePreview;
    private MaterialButton    btnPickImage, btnSubmit;
    private LinearProgressIndicator progressBar;

    private FusedLocationProviderClient fusedLocation;
    private DatabaseHelper              db;
    private SessionManager              session;

    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private String selectedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        db            = DatabaseHelper.getInstance(this);
        session       = new SessionManager(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        Toolbar toolbar = findViewById(R.id.toolbarReport);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.report_title);
        }

        bindViews();
        detectLocation();
        setListeners();
    }

    private void bindViews() {
        tilTitle       = findViewById(R.id.tilReportTitle);
        tilDescription = findViewById(R.id.tilReportDescription);
        etTitle        = findViewById(R.id.etReportTitle);
        etDescription  = findViewById(R.id.etReportDescription);
        rgType         = findViewById(R.id.rgReportType);
        rgSeverity     = findViewById(R.id.rgSeverity);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        ivImagePreview = findViewById(R.id.ivReportImagePreview);
        btnPickImage   = findViewById(R.id.btnPickImage);
        btnSubmit      = findViewById(R.id.btnSubmitReport);
        progressBar    = findViewById(R.id.progressReport);
    }

    private void setListeners() {
        btnPickImage.setOnClickListener(v -> requestImagePick());
        btnSubmit.setOnClickListener(v   -> submitReport());
    }

    // ── Location ──────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void detectLocation() {
        tvLocationStatus.setText(R.string.detecting_location);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocation.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    tvLocationStatus.setText(getString(R.string.location_detected)
                            + String.format(Locale.getDefault(), "\n%.5f, %.5f",
                            currentLat, currentLng));
                } else {
                    tvLocationStatus.setText(R.string.error_location);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQUEST_LOCATION_PERMISSION);
        }
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    private void requestImagePick() {
        // For API 33+ use READ_MEDIA_IMAGES; below that READ_EXTERNAL_STORAGE
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, Constants.REQUEST_STORAGE_PERMISSION);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), Constants.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                selectedImagePath = copyImageToStorage(imageUri);
                ivImagePreview.setImageURI(imageUri);
                ivImagePreview.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Copy a gallery image URI into the app's private files directory. */
    private String copyImageToStorage(Uri uri) throws IOException {
        File imagesDir = new File(getFilesDir(), Constants.IMAGE_DIR);
        if (!imagesDir.exists()) imagesDir.mkdirs();

        String filename = "report_" + System.currentTimeMillis() + ".jpg";
        File outFile = new File(imagesDir, filename);

        try (InputStream in  = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("Cannot open input stream for URI");
            byte[] buf = new byte[4096];
            int    len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        return outFile.getAbsolutePath();
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void submitReport() {
        tilTitle.setError(null);

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (TextUtils.isEmpty(title)) {
            tilTitle.setError(getString(R.string.error_empty_fields));
            etTitle.requestFocus();
            return;
        }

        if (currentLat == 0.0 && currentLng == 0.0) {
            Toast.makeText(this, getString(R.string.error_location), Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine type
        String type;
        int typeId = rgType.getCheckedRadioButtonId();
        if      (typeId == R.id.rbAccident)     type = Report.TYPE_ACCIDENT;
        else if (typeId == R.id.rbPothole)      type = Report.TYPE_POTHOLE;
        else if (typeId == R.id.rbConstruction) type = Report.TYPE_CONSTRUCTION;
        else                                    type = Report.TYPE_TRAFFIC;

        // Determine severity
        int severity;
        int sevId = rgSeverity.getCheckedRadioButtonId();
        if      (sevId == R.id.rbSeverityHigh)   severity = Report.SEVERITY_HIGH;
        else if (sevId == R.id.rbSeverityMedium) severity = Report.SEVERITY_MEDIUM;
        else                                     severity = Report.SEVERITY_LOW;

        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());

        Report report = new Report(
                session.getUserId(), type, title, description,
                currentLat, currentLng, severity, timestamp);
        report.setImagePath(selectedImagePath);

        showLoading(true);
        new Thread(() -> {
            long id = db.insertReport(report);
            runOnUiThread(() -> {
                showLoading(false);
                if (id > 0) {
                    NotificationHelper.showReportSubmitted(this, report.getTypeLabel(), title);
                    Toast.makeText(this, R.string.report_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save report. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ── Permission callback ───────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) return;
        if (requestCode == Constants.REQUEST_LOCATION_PERMISSION)  detectLocation();
        if (requestCode == Constants.REQUEST_STORAGE_PERMISSION)   launchImagePicker();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
    }
}
