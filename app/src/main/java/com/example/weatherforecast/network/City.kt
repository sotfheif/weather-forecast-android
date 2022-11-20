package com.example.weatherforecast.network

data class City (
        val id: Int? = null,
        val name: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val elevation: Double? = null,
        val timezone: String? = null,
        val feature_code: String? = null,
        val country_code: String? = null,
        val country: String? = null,
        val country_id: Int? = null,
        val population: Int? = null,
        val postcodes: List<String> = listOf(),
        val admin1: String? = null,
        val admin2: String? = null,
        val admin3: String? = null,
        val admin4: String? = null,
        val admin1_id: Int? = null,
        val admin2_id: Int? = null,
        val admin3_id: Int? = null,
        val admin4_id: Int? = null
        )