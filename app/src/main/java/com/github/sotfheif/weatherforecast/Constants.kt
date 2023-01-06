package com.github.sotfheif.weatherforecast

object Constants {
    const val MAX_LOC_AGE = 1000 * 60 * 5
    const val WEEK_IN_MILLIS = 1000 * 60 * 60 * 24 * 7
    const val DETECT_GEO_TIMEOUT_CHECK_PERIOD_IN_MILLIS: Long =
        500 // THIS SHOULD DIVIDE DETECT_GEO_TIMEOUT_IN_MILLIS
    const val DETECT_GEO_TIMEOUT_CHECK_TIMES_NETWORK = 20
    const val DETECT_GEO_TIMEOUT_CHECK_TIMES_GPS = 60

    //const val DETECT_GEO_TIMEOUT_IN_MILLIS: Long = DETECT_GEO_TIMEOUT_CHECK_PERIOD_IN_MILLIS * DETECT_GEO_TIMEOUT_CHECK_TIMES
    const val REQUEST_LOCATION_UPDATES_MIN_TIME_MS: Long = 1000 * 60 * 5//was 500
    const val REQUEST_LOCATION_UPDATES_MIN_DISTANCE_M: Float = 0F

    /*
    const val CITY_CLIENT_CONNECT_TIMEOUT_MS: Long = 8_000L
    const val CITY_CLIENT_READ_TIMEOUT_MS: Long = 8_000L
     */
    const val CITY_CLIENT_CALL_TIMEOUT_MS: Long = 15_000L

    /*
    const val FORECAST_CLIENT_CONNECT_TIMEOUT_MS: Long = 8_000L
    const val FORECAST_CLIENT_READ_TIMEOUT_MS: Long = 6_000L
    */
    const val FORECAST_CLIENT_CALL_TIMEOUT_MS: Long = 12_000L
    const val EMPTY_EXCEPTION: String = "no exception"
}