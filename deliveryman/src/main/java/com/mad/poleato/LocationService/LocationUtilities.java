package com.mad.poleato.LocationService;

import com.google.android.gms.maps.model.LatLng;

/**
 * Class that compute distance with latitude and longitude given
 */

public class LocationUtilities {


    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM


    public static double computeDistance(LatLng origin, LatLng destination) {

        double dLat  = Math.toRadians((destination.latitude - origin.latitude));
        double dLong = Math.toRadians((destination.longitude - origin.longitude));

        double a = haversin(dLat) + Math.cos(origin.latitude) * Math.cos(destination.latitude) * haversin(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS * c;
        return distance;
    }


    public static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }


}
