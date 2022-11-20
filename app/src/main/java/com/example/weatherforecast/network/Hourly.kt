package com.example.weatherforecast.network

data class Hourly (
    val time: List<String> = listOf(),
    val pressure_msl: List<Double> = listOf(),
    val relativehumidity_2m: List<Int> = listOf()
)