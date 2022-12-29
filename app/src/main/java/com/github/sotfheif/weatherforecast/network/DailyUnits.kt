package com.github.sotfheif.weatherforecast.network

data class DailyUnits (
    val time: String = "iso8601",
    val weathercode: String = "wmo code",
    val temperature_2m_min: String = "°C",
    val temperature_2m_max: String = "°C",
    val windspeed_10m_max: String = "km/h",
    val winddirection_10m_dominant: String = "°"
        )