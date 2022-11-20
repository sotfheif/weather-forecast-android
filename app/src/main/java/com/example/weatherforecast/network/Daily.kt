package com.example.weatherforecast.network

data class Daily (
    val time: List<String> = listOf(),
    val weathercode: List<Int> = listOf(),
    val temperature_2m_min: List<Double> = listOf(),
    val temperature_2m_max: List<Double> = listOf(),
    val windspeed_10m_max: List<Double> = listOf(),
    val winddirection_10m_dominant: List<Int> = listOf()
)