package com.example.saferoutegis.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferoutegis.R;
import com.example.saferoutegis.adapters.AlertsAdapter;
import com.example.saferoutegis.database.DatabaseHelper;
import com.example.saferoutegis.models.Report;

import java.util.ArrayList;
import java.util.List;

/**
 * Alerts screen – shows all road hazard reports in a filterable RecyclerView.
 *
 * Filter tabs: All | Accidents | Potholes | Construction | Traffic
 * Tapping an item opens the map centred on that report.
 */
public class AlertsActivity extends AppCompatActivity {

    public static final String EXTRA_FILTER_TYPE = "filterType";

    private RecyclerView   recyclerView;
    private AlertsAdapter  adapter;
    private LinearLayout   layoutEmpty;

    private final int[] TAB_IDS = {
            R.id.tabAll,
            R.id.tabAccidents,
            R.id.tabPotholes,
            R.id.tabConstruction,
            R.id.tabTraffic
    };

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        db = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbarAlerts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.alerts_title);
        }

        bindViews();
        setupRecyclerView();
        setupFilterTabs();
        loadReports();

        // Apply pre-filter from intent (e.g. opened from a Dashboard card)
        String preFilter = getIntent().getStringExtra(EXTRA_FILTER_TYPE);
        if (preFilter != null) applyFilterTab(preFilter);
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerAlerts);
        layoutEmpty  = findViewById(R.id.layoutAlertsEmpty);
    }

    // ── Filter tabs ───────────────────────────────────────────────────────────

    private void setupFilterTabs() {
        for (int id : TAB_IDS) {
            TextView tab = findViewById(id);
            if (tab == null) continue;
            tab.setOnClickListener(v -> {
                selectTab((TextView) v);
                String tag = (String) v.getTag();
                adapter.filterByType(tag);
                updateEmptyState();
            });
        }
        // "All" is selected by default
        TextView defaultTab = findViewById(R.id.tabAll);
        if (defaultTab != null) selectTab(defaultTab);
    }

    private void selectTab(TextView chosen) {
        for (int id : TAB_IDS) {
            TextView tab = findViewById(id);
            if (tab == null) continue;

            if (tab == chosen) {
                // Selected: solid white pill, primary-blue text, bold
                tab.setBackgroundResource(R.drawable.bg_tab_selected);
                tab.setTextColor(0xFF1565C0);
                tab.setTypeface(null, Typeface.BOLD);
            } else {
                // Unselected: transparent pill with white border, dim white text
                tab.setBackgroundResource(R.drawable.bg_tab_unselected);
                tab.setTextColor(0xCCFFFFFF);
                tab.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    /**
     * Programmatically select the tab that matches the given report type and filter the list.
     */
    private void applyFilterTab(String type) {
        int tabId = R.id.tabAll;
        switch (type) {
            case Report.TYPE_ACCIDENT:     tabId = R.id.tabAccidents;    break;
            case Report.TYPE_POTHOLE:      tabId = R.id.tabPotholes;     break;
            case Report.TYPE_CONSTRUCTION: tabId = R.id.tabConstruction; break;
            case Report.TYPE_TRAFFIC:      tabId = R.id.tabTraffic;      break;
        }
        TextView tab = findViewById(tabId);
        if (tab != null) {
            selectTab(tab);
            adapter.filterByType(type);
            updateEmptyState();
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new AlertsAdapter(this, new ArrayList<>());
        adapter.setOnItemClickListener(this::openOnMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
        boolean empty = adapter.getItemCount() == 0;
        if (layoutEmpty != null) layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Map navigation ────────────────────────────────────────────────────────

    private void openOnMap(Report report) {
        Toast.makeText(this,
                "Location: " + String.format("%.5f, %.5f",
                        report.getLatitude(), report.getLongitude()),
                Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MapActivity.class));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
