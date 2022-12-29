package com.github.sotfheif.weatherforecast.network

data class CityResponse (
    val results: List<City> = listOf(),
    val generationtime_ms: Double = 0.0
)