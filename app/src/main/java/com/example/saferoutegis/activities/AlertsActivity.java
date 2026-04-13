package com.example.saferoutegis.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferoutegis.R;
import com.example.saferoutegis.adapters.AlertsAdapter;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.Report;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Alerts screen – shows all road hazard reports in a filterable RecyclerView.
 *
 * Filter chips: All | Accidents | Potholes | Construction | Traffic
 * Tapping an item opens the map centred on that report.
 */
public class AlertsActivity extends AppCompatActivity {

    public static final String EXTRA_FILTER_TYPE = "filterType";

    private RecyclerView  recyclerView;
    private AlertsAdapter adapter;
    private LinearLayout  layoutEmpty;
    private ChipGroup     chipGroup;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        db = DatabaseHelper.getInstance(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarAlerts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.alerts_title);
        }

        bindViews();
        setupRecyclerView();
        loadReports();

        // Apply pre-filter from intent (e.g. opened from Dashboard card)
        String preFilter = getIntent().getStringExtra(EXTRA_FILTER_TYPE);
        if (preFilter != null) applyFilterChip(preFilter);
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerAlerts);
        layoutEmpty  = findViewById(R.id.layoutAlertsEmpty);
        chipGroup    = findViewById(R.id.chipGroupFilter);

        // Force chip colors programmatically as safety measure
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        // Text Color CSL
        int[] textColors = new int[]{
                0xFFFFFFFF,  // White when checked
                0xDDFFFFFF   // Semi-white when unchecked
        };
        android.content.res.ColorStateList chipTextCSL =
                new android.content.res.ColorStateList(states, textColors);

        // Background Color CSL
        int[] bgColors = new int[]{
                0xFF1976D2,  // Primary Blue when checked
                0x33FFFFFF   // 20% White when unchecked
        };
        android.content.res.ColorStateList chipBgCSL =
                new android.content.res.ColorStateList(states, bgColors);

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            if (chipGroup.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                chip.setTextColor(chipTextCSL);
                chip.setChipBackgroundColor(chipBgCSL);
                // Also set stroke for unselected state
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0x55FFFFFF));
                chip.setChipStrokeWidth(1f);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new AlertsAdapter(this, new ArrayList<>());
        adapter.setOnItemClickListener(this::openOnMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Filter chip listener
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = findViewById(checkedIds.get(0));
            String tag = chip != null ? (String) chip.getTag() : null;
            adapter.filterByType(tag);
            updateEmptyState();
        });
    }

    private void loadReports() {
        new Thread(() -> {
            List<Report> reports = db.getAllReports();
            runOnUiThread(() -> {
                adapter.updateData(reports);
                updateEmptyState();
            });
        }).start();
    }

    private void updateEmptyState() {
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    /**
     * Select the chip matching a given type and scroll to it.
     */
    private void applyFilterChip(String type) {
        int chipId = -1;
        switch (type) {
            case Report.TYPE_ACCIDENT:     chipId = R.id.chipAccidents;     break;
            case Report.TYPE_POTHOLE:      chipId = R.id.chipPotholes;      break;
            case Report.TYPE_CONSTRUCTION: chipId = R.id.chipConstruction;  break;
            case Report.TYPE_TRAFFIC:      chipId = R.id.chipTraffic;       break;
        }
        if (chipId != -1) {
            Chip chip = findViewById(chipId);
            if (chip != null) chip.setChecked(true);
        }
    }

    /** Open MapActivity and pass the report's coordinates so it can be highlighted. */
    private void openOnMap(Report report) {
        Toast.makeText(this,
                "Location: " + String.format("%.5f, %.5f",
                        report.getLatitude(), report.getLongitude()),
                Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MapActivity.class));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
