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

private const val TAG = "MainViewModel"
class MainViewModel : ViewModel() {
    lateinit var requestLocPermissionLauncher: ActivityResultLauncher<String>


    private val _statusImageMainFragment = MutableLiveData<Boolean>()
    val statusImageMainFragment: LiveData<Boolean>
        get() = _statusImageMainFragment
    private val _statusImageCityFragment = MutableLiveData<Boolean>()
    val statusImageCityFragment: LiveData<Boolean>
        get() = _statusImageCityFragment

    private var _getForecastResult: ForecastResponse = ForecastResponse()
    val getForecastResult: ForecastResponse
        get() = _getForecastResult
    private var _selectedCity: City = City()
    val selectedCity: City
        get() = _selectedCity
    private var _weekForecast: List<DayForecast> = listOf()
    val weekForecast: List<DayForecast>
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

    val emptyCity = City()

    /*
        enum class LocSetOptions {
            CURRENT, SELECT
        }
        private var _locationSettingOption: LocSetOptions = LocSetOptions.CURRENT
        val locationSettingOption: LocSetOptions
            get() = _locationSettingOption

        fun setLocOption(locSetOption: LocSetOptions) {
            _locationSettingOption = locSetOption
        }
    */
    fun setLocation(location: Location) {
        _currentLocation = location
        latitude = _currentLocation.latitude
        longitude = _currentLocation.longitude
    }

    suspend fun getCitiesByName(query: String): Pair<Boolean, String> {
        Log.d(TAG, "entered getcitiesbyname")
        var listResult = CityResponse()
        var exception: String = Constants.emptyException
        val getCitiesJob = viewModelScope.launch {
            _citySearchResult = listOf()
            setSpinnerVisibilityCityFragment(true)
            try {
                listResult = OpenMeteoApi.retrofitCityService.getCityResponse(name = query)
            } catch (e: Exception) {
                Log.d("getCitiesByName", e.toString())
                exception = e.toString()
            } finally {
                setSpinnerVisibilityCityFragment(false)
            }
        }
        getCitiesJob.join()
        Log.d(TAG, "getcitiesbyname before return")
        return if (listResult.results.isNotEmpty()) {
            _citySearchResult = listResult.results
            Pair(true, exception)
        } else {
            Pair(false, exception)
        }
    }

    @SuppressLint("SimpleDateFormat")
    suspend fun getForecastByCoords(cityLatitude: Double, cityLongitude: Double): String {
        var exception = Constants.emptyException
        viewModelScope.launch {
            val currentDate: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val weekLaterDate: String = SimpleDateFormat("yyyy-MM-dd").format(
                Calendar.getInstance().timeInMillis + Constants.WEEK_IN_MILLIS
            )
            Log.d("viewModel", "before getforecastresponse")
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
                _getForecastResult = listResult
            } catch (e: Exception) {
                Log.d("getforecastresp", e.toString())
                exception = e.toString()
            } finally {
                //setSpinnerVisibilityMainFragment(false)
            }
        }.join()
        Log.d("viewModel", "after getforecastresponse")
        return exception
    }

    fun prepCityForUi(city: City): String {
        return listOfNotNull(
            city.name, city.admin4, city.admin3,
            city.admin2, city.admin1, city.country,
        )
            .joinToString(", ")
    }

    fun setSelectedCity(city: City) {
        _selectedCity = city
    }

    fun resetSelectedCity() {
        _selectedCity = emptyCity
    }

    fun setWeekForecast(weekForecastp: List<DayForecast>) {
        _weekForecast = weekForecastp
    }

    fun resetWeekForecast() {
        _weekForecast = listOf()
    }
    fun resetForecastResult() {
        _getForecastResult = ForecastResponse()
    }

    fun setSpinnerVisibilityMainFragment(b: Boolean) { //replaced by manually setting view.visibility in mainframent
        _statusImageMainFragment.value = b
    }

    private fun setSpinnerVisibilityCityFragment(b: Boolean) { //replaced by manually setting view.visibility in cityframent
        _statusImageCityFragment.value = b
    }


}