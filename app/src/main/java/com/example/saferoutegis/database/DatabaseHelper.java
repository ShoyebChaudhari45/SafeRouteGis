package com.example.saferoutegis.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.saferoutegis.models.Report;
import com.example.saferoutegis.models.User;
import com.example.saferoutegis.utils.LocationHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database helper for Road Safety GIS.
 *
 * Tables:
 *   users   – registered accounts
 *   reports – road hazard reports (accidents, potholes, construction, traffic)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "saferoutegis.db";
    private static final int    DB_VERSION = 1;

    // ── Table: users ──────────────────────────────────────────────────────────
    static final String TABLE_USERS      = "users";
    static final String COL_U_ID         = "id";
    static final String COL_U_NAME       = "name";
    static final String COL_U_EMAIL      = "email";
    static final String COL_U_PASSWORD   = "password";
    static final String COL_U_PHONE      = "phone";
    static final String COL_U_CREATED_AT = "created_at";

    // ── Table: reports ────────────────────────────────────────────────────────
    static final String TABLE_REPORTS      = "reports";
    static final String COL_R_ID           = "id";
    static final String COL_R_USER_ID      = "user_id";
    static final String COL_R_TYPE         = "type";
    static final String COL_R_TITLE        = "title";
    static final String COL_R_DESCRIPTION  = "description";
    static final String COL_R_LATITUDE     = "latitude";
    static final String COL_R_LONGITUDE    = "longitude";
    static final String COL_R_IMAGE_PATH   = "image_path";
    static final String COL_R_SEVERITY     = "severity";
    static final String COL_R_TIMESTAMP    = "timestamp";
    static final String COL_R_IS_ACTIVE    = "is_active";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COL_U_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_U_NAME       + " TEXT NOT NULL,"
                + COL_U_EMAIL      + " TEXT UNIQUE NOT NULL,"
                + COL_U_PASSWORD   + " TEXT NOT NULL,"
                + COL_U_PHONE      + " TEXT,"
                + COL_U_CREATED_AT + " TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_REPORTS + " ("
                + COL_R_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_R_USER_ID     + " INTEGER,"
                + COL_R_TYPE        + " TEXT NOT NULL,"
                + COL_R_TITLE       + " TEXT NOT NULL,"
                + COL_R_DESCRIPTION + " TEXT,"
                + COL_R_LATITUDE    + " REAL NOT NULL,"
                + COL_R_LONGITUDE   + " REAL NOT NULL,"
                + COL_R_IMAGE_PATH  + " TEXT,"
                + COL_R_SEVERITY    + " INTEGER DEFAULT 1,"
                + COL_R_TIMESTAMP   + " TEXT NOT NULL,"
                + COL_R_IS_ACTIVE   + " INTEGER DEFAULT 1,"
                + "FOREIGN KEY (" + COL_R_USER_ID + ") REFERENCES "
                + TABLE_USERS + "(" + COL_U_ID + ")"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPORTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // USER OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /** Insert a new user. Returns the new row ID, or -1 on error. */
    public long insertUser(User user) {
        ContentValues cv = new ContentValues();
        cv.put(COL_U_NAME,       user.getName());
        cv.put(COL_U_EMAIL,      user.getEmail());
        cv.put(COL_U_PASSWORD,   user.getPassword());
        cv.put(COL_U_PHONE,      user.getPhone());
        cv.put(COL_U_CREATED_AT, user.getCreatedAt());
        return getWritableDatabase().insert(TABLE_USERS, null, cv);
    }

    /** Find a user by email. Returns null if not found. */
    public User getUserByEmail(String email) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_U_EMAIL + " = ?",
                new String[]{email});
        User user = null;
        if (c.moveToFirst()) user = cursorToUser(c);
        c.close();
        return user;
    }

    /** Find a user by primary-key ID. Returns null if not found. */
    public User getUserById(int id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_U_ID + " = ?",
                new String[]{String.valueOf(id)});
        User user = null;
        if (c.moveToFirst()) user = cursorToUser(c);
        c.close();
        return user;
    }

    /** Update a user's name and phone. Returns number of rows updated. */
    public int updateUser(User user) {
        ContentValues cv = new ContentValues();
        cv.put(COL_U_NAME,  user.getName());
        cv.put(COL_U_PHONE, user.getPhone());
        return getWritableDatabase().update(TABLE_USERS, cv,
                COL_U_ID + " = ?", new String[]{String.valueOf(user.getId())});
    }

    /** True if an email address is already registered. */
    public boolean isEmailRegistered(String email) {
        return getUserByEmail(email) != null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REPORT OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /** Insert a new report. Returns the new row ID, or -1 on error. */
    public long insertReport(Report report) {
        ContentValues cv = new ContentValues();
        cv.put(COL_R_USER_ID,     report.getUserId());
        cv.put(COL_R_TYPE,        report.getType());
        cv.put(COL_R_TITLE,       report.getTitle());
        cv.put(COL_R_DESCRIPTION, report.getDescription());
        cv.put(COL_R_LATITUDE,    report.getLatitude());
        cv.put(COL_R_LONGITUDE,   report.getLongitude());
        cv.put(COL_R_IMAGE_PATH,  report.getImagePath());
        cv.put(COL_R_SEVERITY,    report.getSeverity());
        cv.put(COL_R_TIMESTAMP,   report.getTimestamp());
        cv.put(COL_R_IS_ACTIVE,   1);
        return getWritableDatabase().insert(TABLE_REPORTS, null, cv);
    }

    /** All active reports, newest first. */
    public List<Report> getAllReports() {
        String sql = "SELECT r.*, u." + COL_U_NAME + " AS reporter_name"
                + " FROM " + TABLE_REPORTS + " r"
                + " LEFT JOIN " + TABLE_USERS + " u ON r." + COL_R_USER_ID + " = u." + COL_U_ID
                + " WHERE r." + COL_R_IS_ACTIVE + " = 1"
                + " ORDER BY r." + COL_R_TIMESTAMP + " DESC";
        return fetchReports(sql, null);
    }

    /** Active reports of a specific type (e.g. "ACCIDENT"). */
    public List<Report> getReportsByType(String type) {
        String sql = "SELECT r.*, u." + COL_U_NAME + " AS reporter_name"
                + " FROM " + TABLE_REPORTS + " r"
                + " LEFT JOIN " + TABLE_USERS + " u ON r." + COL_R_USER_ID + " = u." + COL_U_ID
                + " WHERE r." + COL_R_IS_ACTIVE + " = 1 AND r." + COL_R_TYPE + " = ?"
                + " ORDER BY r." + COL_R_TIMESTAMP + " DESC";
        return fetchReports(sql, new String[]{type});
    }

    /** All reports submitted by a specific user. */
    public List<Report> getReportsByUserId(int userId) {
        String sql = "SELECT r.*, u." + COL_U_NAME + " AS reporter_name"
                + " FROM " + TABLE_REPORTS + " r"
                + " LEFT JOIN " + TABLE_USERS + " u ON r." + COL_R_USER_ID + " = u." + COL_U_ID
                + " WHERE r." + COL_R_USER_ID + " = ?"
                + " ORDER BY r." + COL_R_TIMESTAMP + " DESC";
        return fetchReports(sql, new String[]{String.valueOf(userId)});
    }

    /**
     * Active reports within radiusKm of a given location.
     * Filtering is done in-memory using the Haversine formula.
     */
    public List<Report> getNearbyReports(double lat, double lng, double radiusKm) {
        List<Report> all    = getAllReports();
        List<Report> nearby = new ArrayList<>();
        for (Report r : all) {
            if (LocationHelper.isWithinRadius(lat, lng, r.getLatitude(), r.getLongitude(), radiusKm)) {
                nearby.add(r);
            }
        }
        return nearby;
    }

    /** Number of reports submitted by a user. */
    public int getReportCountByUserId(int userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_REPORTS + " WHERE " + COL_R_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Report> fetchReports(String sql, String[] args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        List<Report> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(cursorToReport(c));
        }
        c.close();
        return list;
    }

    private User cursorToUser(Cursor c) {
        User u = new User();
        u.setId(       c.getInt(   c.getColumnIndexOrThrow(COL_U_ID)));
        u.setName(     c.getString(c.getColumnIndexOrThrow(COL_U_NAME)));
        u.setEmail(    c.getString(c.getColumnIndexOrThrow(COL_U_EMAIL)));
        u.setPassword( c.getString(c.getColumnIndexOrThrow(COL_U_PASSWORD)));
        u.setPhone(    c.getString(c.getColumnIndexOrThrow(COL_U_PHONE)));
        u.setCreatedAt(c.getString(c.getColumnIndexOrThrow(COL_U_CREATED_AT)));
        return u;
    }

    private Report cursorToReport(Cursor c) {
        Report r = new Report();
        r.setId(         c.getInt(   c.getColumnIndexOrThrow(COL_R_ID)));
        r.setUserId(     c.getInt(   c.getColumnIndexOrThrow(COL_R_USER_ID)));
        r.setType(       c.getString(c.getColumnIndexOrThrow(COL_R_TYPE)));
        r.setTitle(      c.getString(c.getColumnIndexOrThrow(COL_R_TITLE)));
        r.setDescription(c.getString(c.getColumnIndexOrThrow(COL_R_DESCRIPTION)));
        r.setLatitude(   c.getDouble(c.getColumnIndexOrThrow(COL_R_LATITUDE)));
        r.setLongitude(  c.getDouble(c.getColumnIndexOrThrow(COL_R_LONGITUDE)));
        r.setImagePath(  c.getString(c.getColumnIndexOrThrow(COL_R_IMAGE_PATH)));
        r.setSeverity(   c.getInt(   c.getColumnIndexOrThrow(COL_R_SEVERITY)));
        r.setTimestamp(  c.getString(c.getColumnIndexOrThrow(COL_R_TIMESTAMP)));
        r.setActive(     c.getInt(   c.getColumnIndexOrThrow(COL_R_IS_ACTIVE)) == 1);

        int nameIdx = c.getColumnIndex("reporter_name");
        r.setUserName(nameIdx >= 0 && !c.isNull(nameIdx) ? c.getString(nameIdx) : "Unknown");
        return r;
    }
}
