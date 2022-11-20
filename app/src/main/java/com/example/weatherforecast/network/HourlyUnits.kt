package com.example.weatherforecast.network

data class HourlyUnits (
    val time: String = "iso8601",
    val pressure_msl: String = "hPa",
    val relativehumidity_2m: String = "%"
)