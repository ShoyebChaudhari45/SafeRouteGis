package com.example.saferoutegis.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferoutegis.R;
import com.example.saferoutegis.models.Report;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying road safety reports in the Alerts screen.
 * Supports click callbacks and in-place filtering by report type.
 */
public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {

    // ── Interface ─────────────────────────────────────────────────────────────

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Context            context;
    private       List<Report>       displayList;  // currently shown
    private       List<Report>       fullList;     // backing store (all items)
    private       OnItemClickListener listener;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AlertsAdapter(Context context, List<Report> reports) {
        this.context     = context;
        this.fullList    = new ArrayList<>(reports);
        this.displayList = new ArrayList<>(reports);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // ── RecyclerView overrides ────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = displayList.get(position);
        holder.bind(report);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(report);
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // ── Public methods ────────────────────────────────────────────────────────

    /** Replace the full dataset and refresh. */
    public void updateData(List<Report> newReports) {
        fullList    = new ArrayList<>(newReports);
        displayList = new ArrayList<>(newReports);
        notifyDataSetChanged();
    }

    /**
     * Filter the displayed list by report type.
     * Pass null or "ALL" to show everything.
     */
    public void filterByType(String type) {
        displayList.clear();
        if (type == null || type.equalsIgnoreCase("ALL")) {
            displayList.addAll(fullList);
        } else {
            for (Report r : fullList) {
                if (r.getType().equalsIgnoreCase(type)) {
                    displayList.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final View      typeIndicator;
        private final ImageView iconView;
        private final TextView  tvTitle;
        private final TextView  tvType;
        private final TextView  tvDescription;
        private final TextView  tvTimestamp;
        private final TextView  tvReporter;
        private final TextView  tvSeverity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIndicator = itemView.findViewById(R.id.viewTypeIndicator);
            iconView      = itemView.findViewById(R.id.ivAlertIcon);
            tvTitle       = itemView.findViewById(R.id.tvAlertTitle);
            tvType        = itemView.findViewById(R.id.tvAlertType);
            tvDescription = itemView.findViewById(R.id.tvAlertDescription);
            tvTimestamp    = itemView.findViewById(R.id.tvAlertTimestamp);
            tvReporter    = itemView.findViewById(R.id.tvAlertReporter);
            tvSeverity    = itemView.findViewById(R.id.tvAlertSeverity);
        }

        void bind(Report report) {
            tvTitle.setText(report.getTitle());
            tvTimestamp.setText(report.getTimestamp());

            // Description (hide if empty)
            String desc = report.getDescription();
            if (desc != null && !desc.trim().isEmpty()) {
                tvDescription.setText(desc);
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Reporter name
            String name = report.getUserName();
            tvReporter.setText("By: " + (name != null ? name : "Unknown"));

            // ── Type-specific colors ──
            int typeColor = typeColor(report.getType());

            // Type indicator bar (left edge)
            typeIndicator.setBackgroundColor(typeColor);

            // Type badge — VISIBLE with colored background + white text
            tvType.setText(report.getTypeLabel());
            tvType.setTextColor(Color.WHITE);
            tvType.setVisibility(View.VISIBLE);
            GradientDrawable typeBg = new GradientDrawable();
            typeBg.setColor(typeColor);
            typeBg.setCornerRadius(24f);
            tvType.setBackground(typeBg);

            // Severity badge — colored pill
            tvSeverity.setText(report.getSeverityLabel());
            tvSeverity.setTextColor(Color.WHITE);
            tvSeverity.setVisibility(View.VISIBLE);
            GradientDrawable severityBg = new GradientDrawable();
            severityBg.setColor(severityColor(report.getSeverity()));
            severityBg.setCornerRadius(24f);
            tvSeverity.setBackground(severityBg);

            // Icon
            iconView.setImageResource(typeIcon(report.getType()));
            iconView.setColorFilter(typeColor);
            iconView.setVisibility(View.VISIBLE);
        }

        private int typeColor(String type) {
            if (type == null) return Color.GRAY;
            switch (type) {
                case Report.TYPE_ACCIDENT:     return Color.parseColor("#D32F2F");
                case Report.TYPE_POTHOLE:      return Color.parseColor("#E65100");
                case Report.TYPE_CONSTRUCTION: return Color.parseColor("#EF6C00");
                case Report.TYPE_TRAFFIC:      return Color.parseColor("#1565C0");
                default:                       return Color.GRAY;
            }
        }

        private int typeIcon(String type) {
            if (type == null) return R.drawable.ic_feature_map;
            switch (type) {
                case Report.TYPE_ACCIDENT:     return R.drawable.ic_feature_accident;
                case Report.TYPE_POTHOLE:      return R.drawable.ic_feature_pothole;
                case Report.TYPE_CONSTRUCTION: return R.drawable.ic_feature_construction;
                case Report.TYPE_TRAFFIC:      return R.drawable.ic_feature_traffic;
                default:                       return R.drawable.ic_feature_map;
            }
        }

        private int severityColor(int severity) {
            switch (severity) {
                case Report.SEVERITY_HIGH:   return Color.parseColor("#D32F2F");
                case Report.SEVERITY_MEDIUM: return Color.parseColor("#EF6C00");
                default:                     return Color.parseColor("#2E7D32");
            }
        }
    }
}
