package com.example.saferoutegis.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing and verifying passwords using SHA-256.
 * Passwords are never stored in plain text.
 */
public final class PasswordUtils {

    private PasswordUtils() {}

    /**
     * Hash a plain-text password with SHA-256.
     *
     * @param plaintext raw password string
     * @return 64-character hex string representing the hash
     */
    public static String hashPassword(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every JVM/Android platform
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verify a plain-text password against a stored SHA-256 hash.
     *
     * @param plaintext      password entered by the user
     * @param storedHash     hash from the database
     * @return true if the password matches
     */
    public static boolean verifyPassword(String plaintext, String storedHash) {
        return hashPassword(plaintext).equals(storedHash);
    }
}
