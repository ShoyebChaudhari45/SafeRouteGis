package com.example.saferoutegis.utils;

/**
 * Utility methods for geographic calculations.
 */
public final class LocationHelper {

    private LocationHelper() {}

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate the distance between two lat/lng points using the Haversine formula.
     *
     * @return distance in kilometres
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check whether a point lies within a given radius of a centre point.
     */
    public static boolean isWithinRadius(double centerLat, double centerLng,
                                          double pointLat,  double pointLng,
                                          double radiusKm) {
        return distanceKm(centerLat, centerLng, pointLat, pointLng) <= radiusKm;
    }

    /**
     * Format a lat/lng pair as a string suitable for the Directions API.
     */
    public static String toLatLngString(double lat, double lng) {
        return lat + "," + lng;
    }
}
