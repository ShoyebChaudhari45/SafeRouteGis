package com.example.saferoutegis.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the user login session using SharedPreferences.
 * Stores the currently logged-in user's ID, email, and name.
 */
public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Call this after a successful login or registration.
     */
    public void createLoginSession(int userId, String email, String name) {
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.putInt(Constants.KEY_USER_ID, userId);
        editor.putString(Constants.KEY_USER_EMAIL, email);
        editor.putString(Constants.KEY_USER_NAME, name);
        editor.apply();
    }

    /** @return true if a user is currently logged in */
    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    /** @return the logged-in user's database ID, or -1 if not logged in */
    public int getUserId() {
        return prefs.getInt(Constants.KEY_USER_ID, -1);
    }

    /** @return the logged-in user's email */
    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, "");
    }

    /** @return the logged-in user's display name */
    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, "User");
    }

    /** Update the stored display name (after profile edit) */
    public void updateUserName(String name) {
        editor.putString(Constants.KEY_USER_NAME, name);
        editor.apply();
    }

    /** Clear the session (logout) */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
