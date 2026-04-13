package com.example.saferoutegis.models;

/**
 * Model class representing a road safety report (accident, pothole, construction, traffic).
 * Stored in the SQLite 'reports' table.
 */
public class Report {

    // Report type constants
    public static final String TYPE_ACCIDENT     = "ACCIDENT";
    public static final String TYPE_POTHOLE      = "POTHOLE";
    public static final String TYPE_CONSTRUCTION = "CONSTRUCTION";
    public static final String TYPE_TRAFFIC      = "TRAFFIC";

    // Severity constants
    public static final int SEVERITY_LOW    = 1;
    public static final int SEVERITY_MEDIUM = 2;
    public static final int SEVERITY_HIGH   = 3;

    private int    id;
    private int    userId;
    private String userName;   // resolved from users table for display
    private String type;       // one of the TYPE_ constants above
    private String title;
    private String description;
    private double latitude;
    private double longitude;
    private String imagePath;  // local file path, may be null
    private int    severity;   // 1 = Low, 2 = Medium, 3 = High
    private String timestamp;  // ISO-8601 string, e.g. "2024-06-01 14:30:00"
    private boolean active;    // false = resolved/removed

    // ── Constructors ──────────────────────────────────────────────────────────

    public Report() {
        this.active = true;
        this.severity = SEVERITY_LOW;
    }

    public Report(int userId, String type, String title, String description,
                  double latitude, double longitude, int severity, String timestamp) {
        this.userId      = userId;
        this.type        = type;
        this.title       = title;
        this.description = description;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.severity    = severity;
        this.timestamp   = timestamp;
        this.active      = true;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Human-readable severity label */
    public String getSeverityLabel() {
        switch (severity) {
            case SEVERITY_HIGH:   return "High";
            case SEVERITY_MEDIUM: return "Medium";
            default:              return "Low";
        }
    }

    /** Human-readable type label */
    public String getTypeLabel() {
        switch (type) {
            case TYPE_ACCIDENT:     return "Accident";
            case TYPE_POTHOLE:      return "Pothole";
            case TYPE_CONSTRUCTION: return "Construction";
            case TYPE_TRAFFIC:      return "Traffic";
            default:                return type;
        }
    }

    @Override
    public String toString() {
        return "Report{id=" + id + ", type='" + type + "', title='" + title + "'}";
    }
}
