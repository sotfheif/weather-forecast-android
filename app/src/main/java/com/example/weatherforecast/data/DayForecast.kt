package com.example.weatherforecast.data

data class DayForecast (
    val id: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pressure: String? = null,
    val relativeHumidity: String? = null,
    val weather: String? = null,
    val temperature2mMin: String? = null,
    val temperature2mMax: String? = null,
    val windspeed10mMax: String? = null,
    val winddirection10mDominant: String? = null,
    val timeStamp: Long? = null
        )