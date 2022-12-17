package com.example.weatherforecast.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


private const val BASE_URL_FORECAST =
    "https://api.open-meteo.com"

private const val BASE_URL_CITY =
    "https://geocoding-api.open-meteo.com"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/* having them here didn't change things like connecttimeout, readtimeout, etc
private val retrofitForecast = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL_FORECAST)
    .build()

private val retrofitCity = Retrofit.Builder()
    .baseUrl(BASE_URL_CITY)
    .client(OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.SECONDS)
        .build())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
*/

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecastResponse(@Query("latitude") latitude:Double,
                                    @Query("longitude") longitude: Double,
                                    @Query("timezone") timezone: String ="auto",
                                    @Query("start_date") start_date: String,
                                    @Query("end_date") end_date: String,
                                    @Query(value = "hourly", encoded = true) hourly: String,
                                    @Query(value = "daily", encoded = true) daily: String
    ) : ForecastResponse

    @GET("v1/search")
    suspend fun getCityResponse(@Query("name") name: String,
                                @Query("count") count: Int = 100,
                                @Query("format") format: String = "json",
                                @Query("language") language: String="en"
    ): CityResponse
}

object OpenMeteoApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.SECONDS)
        .build()

    val retrofitForecastService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_FORECAST)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }
    val retrofitCityService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_CITY)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}