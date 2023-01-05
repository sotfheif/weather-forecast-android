package com.github.sotfheif.weatherforecast

import android.location.Location
import com.github.sotfheif.weatherforecast.data.DayForecast
import com.github.sotfheif.weatherforecast.network.City
import com.github.sotfheif.weatherforecast.network.ForecastResponse
import java.util.*
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

fun Double.toStringBiggerMinus() = if (this < 0.0) "–${abs(this)}" else this.toString()

fun chooseLatestLocation(
    latestKnownLocation1: Location?,
    latestKnownLocation2: Location?
): Location? {
    if (latestKnownLocation1 == null && latestKnownLocation2 == null) {
        return null
    }
    return if ((latestKnownLocation1?.time ?: 0) > (latestKnownLocation2?.time ?: 0)) {
        latestKnownLocation1
    } else latestKnownLocation2
}

fun ForecastResponse.toDayForecastList():
        List<DayForecast> {
    if (latitude == null) {
        return mutableListOf(DayForecast())
    }
    val weekForecast: MutableList<DayForecast> = mutableListOf()
    repeat(7) {
        val dayPressure = round(
            hourly.pressure_msl
                .subList(it * 24, (it + 1) * 24).average() * 10
        ) / 10
        val dayRelativeHumidity = hourly.relativehumidity_2m
            .subList(it * 24, (it + 1) * 24).average().roundToInt()
        weekForecast.add(
            DayForecast(
                latitude = latitude,
                longitude = longitude,
                pressure = dayPressure.toString(),
                relativeHumidity = dayRelativeHumidity.toString(),
                weather = daily.weathercode[it].toString(),
                temperature2mMin = daily.temperature_2m_min[it]
                    .toStringBiggerMinus(),
                temperature2mMax = daily.temperature_2m_max[it]
                    .toStringBiggerMinus(),
                windspeed10mMax = daily.windspeed_10m_max[it].toString(),
                winddirection10mDominant = daily
                    .winddirection_10m_dominant[it].toString(),
                timeStamp = Calendar.getInstance().timeInMillis
            )
        )
    }
    return weekForecast
}

fun City.prepForUi(): String {
    return listOfNotNull(
        name, admin4, admin3,
        admin2, admin1, country,
    )
        .joinToString(", ")
}
/* want to replace prepDayForecastUiText() in main and week fragments with this
fun DayForecast.prepForUi(context: Context,
                          weatherCodeMap: Map<Int, String>): String {
    return context.run {
        getString(
            R.string.day_forecast,
            temperature2mMin?.plus(
                getString(R.string.temperature_unit)
            ) ?: "",
            temperature2mMax?.plus(
                getString(R.string.temperature_unit)
            ) ?: "",
            weatherCodeMap[weather?.toInt()] ?: "",
            pressure?.plus(
                getString(R.string.pressure_unit)
            ) ?: "",
            windspeed10mMax?.plus(
                getString(R.string.wind_speed_unit)
            ) ?: "",
            winddirection10mDominant?.plus(
                getString(R.string.wind_direction_unit)
            ) ?: "",
            relativeHumidity?.plus(
                getString(R.string.relative_humidity_unit)
            ) ?: ""
        )
    }
}
*/