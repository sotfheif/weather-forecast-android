package com.github.sotfheif.weatherforecast.network

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int
) {
}