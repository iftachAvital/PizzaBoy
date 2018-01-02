package com.example.iftach.pizzaboy;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

/**
 * Created by iftach on 27/11/17.
 */

class Constants {
    static final double PIZZA_LATITUDE = 32.046095;
    static final double PIZZA_LONGITUDE = 34.834538;

    static final String MARKERS_LIST_EXTRA = "MARKERS_LIST_EXTRA";
    static final String SHIFT_EXTRA = "SHIFT_EXTRA";

    static final String SHIFT = "SHIFT";

    static final String REQUEST_CODE = "REQUEST_CODE";
    static final String MAP_COMMAND = "MAP_COMMAND";

    static final String SHIFTS_COLLECTION = "shifts";

    static final int RC_SIGN_IN = 1;
    static final int RC_PLACE_AUTOCOMPLETE = 2;
    static final int RC_SHOW_DELIVERIES = 3;
    static final int RC_IN_SHIFT = 5;
    static final int RC_SHOW_SHIFT = 6;
    static final int RC_NEW_DELIVERY = 7;
    static final int RC_LOCATION = 8;

    static final Locale EN_LOCAL = new Locale("en");
    public static final String USERS_COLLENTION = "users";
    public static final String WEEK_DATA = "WEEK_DATA";
    public static final String USER_UID_EXTRA = "USER_UID_EXTRA";
}
