package com.example.weatherforecast.network

data class ForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val generationtime_ms: Double? = null,
    val utc_offset_seconds: Long? = null,
    val timezone: String? = null,
    val timezone_abbreviation: String? = null,
    val elevation: Double? = null,
    val hourly_units: HourlyUnits = HourlyUnits(),
    val hourly: Hourly = Hourly(),
    val daily_units: DailyUnits = DailyUnits(),
    val daily: Daily = Daily()
)