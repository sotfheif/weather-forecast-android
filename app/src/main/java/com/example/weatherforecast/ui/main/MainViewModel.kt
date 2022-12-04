package com.example.weatherforecast.ui.main

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherforecast.Constants
import com.example.weatherforecast.data.DayForecast
import com.example.weatherforecast.network.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel : ViewModel() {
    lateinit var requestLocPermissionLauncher: ActivityResultLauncher<String>


    private val _statusImageMainFragment = MutableLiveData<Boolean>()
    val statusImageMainFragment: LiveData<Boolean>
        get() = _statusImageMainFragment
    private val _statusImageCityFragment = MutableLiveData<Boolean>()
    val statusImageCityFragment: LiveData<Boolean>
        get() = _statusImageCityFragment

    private var _getForecastResult = MutableLiveData<ForecastResponse>()
    val getForecastResult: LiveData<ForecastResponse>
        get() = _getForecastResult
    private val _selectedCity = MutableLiveData<City>()
    val selectedCity: LiveData<City>
        get() = _selectedCity
    private val _weekForecast = MutableLiveData<List<DayForecast>>()
    val weekForecast: LiveData<List<DayForecast>>
        get() = _weekForecast
    private var _citySearchResult = listOf<City>()
    val citySearchResult: List<City> get() = _citySearchResult

    //val cityNotFound = "Nothing found"

    private lateinit var _currentLocation: Location
    val currentLocation: Location
        get() = _currentLocation

    private var latitude: Double? = null
    private var longitude: Double? = null
    private val timezone = "auto"
    private var hourly = "pressure_msl,relativehumidity_2m"
    private var daily =
        "weathercode,temperature_2m_min,temperature_2m_max,windspeed_10m_max,winddirection_10m_dominant"

    enum class LocSetOptions {
        CURRENT, SELECT
    }

    private val _locationSettingOption = MutableLiveData(LocSetOptions.CURRENT)
    val locationSettingOption: LiveData<LocSetOptions>
        get() = _locationSettingOption

    fun setLocOption(locSetOption: LocSetOptions) {
        _locationSettingOption.value = locSetOption
    }

    fun setLocation(location: Location) {
        _currentLocation = location
        latitude = _currentLocation.latitude
        longitude = _currentLocation.longitude
    }

    suspend fun getCitiesByName(query: String): Boolean {
        var listResult = CityResponse()
        val getCitiesJob = viewModelScope.launch {
            _citySearchResult = listOf()
            setSpinnerVisibilityCityFragment(true)
            try {
                listResult = OpenMeteoApi.retrofitCityService.getCityResponse(name = query)
            } catch (d: Exception) {
                Log.d("getCitiesByName", d.toString())
            } finally {
                setSpinnerVisibilityCityFragment(false)
            }
        }
        getCitiesJob.join()
        return if (listResult.results.isNotEmpty()) {
            _citySearchResult = listResult.results
            true
        } else {
            false
        }
    }

    @SuppressLint("SimpleDateFormat")
    suspend fun getForecastByCoords(cityLatitude: Double, cityLongitude: Double) {
        viewModelScope.launch {
            val currentDate: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val weekLaterDate: String = SimpleDateFormat("yyyy-MM-dd").format(
                Calendar.getInstance().timeInMillis + Constants.WEEK_IN_MILLIS
            )

            try {
                val listResult = OpenMeteoApi.retrofitForecastService
                    .getForecastResponse(
                        latitude = cityLatitude,
                        longitude = cityLongitude,
                        timezone = timezone,
                        start_date = currentDate,
                        end_date = weekLaterDate,
                        hourly = hourly,
                        daily = daily
                    )
                _getForecastResult.value = listResult
            } catch (_: Exception) {
            } finally {
                setSpinnerVisibilityMainFragment(false)
            }
        }.join()
    }

    fun prepCityForUi(city: City): String {
        return listOfNotNull(
            city.name,
            city.admin4, city.admin3, city.admin2, city.admin1
        )
            .joinToString(", ")
    }

    fun setSelectedCity(city: City) {
        _selectedCity.value = city
    }

    fun resetSelectedCity() {
        _selectedCity.value = City()
    }

    fun setWeekForecast(weekForecastp: List<DayForecast>) {
        _weekForecast.value = weekForecastp
    }

    fun resetWeekForecast() {
        _weekForecast.value = listOf()
    }
    fun resetForecastResult() {
        _getForecastResult.value = ForecastResponse()
    }

    fun setSpinnerVisibilityMainFragment(b: Boolean) {
        _statusImageMainFragment.value = b
    }

    private fun setSpinnerVisibilityCityFragment(b: Boolean) {
        _statusImageCityFragment.value = b
    }

}