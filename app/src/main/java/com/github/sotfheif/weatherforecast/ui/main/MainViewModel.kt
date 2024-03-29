package com.github.sotfheif.weatherforecast.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.*
import com.github.sotfheif.weatherforecast.Constants
import com.github.sotfheif.weatherforecast.chooseLatestLocation
import com.github.sotfheif.weatherforecast.data.DayForecast
import com.github.sotfheif.weatherforecast.network.*
import com.github.sotfheif.weatherforecast.toDayForecastList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    lateinit var requestLocPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val _forecastStatusImageMainFragment = MutableLiveData<Boolean>()
    val forecastStatusImageMainFragment: LiveData<Boolean>
        get() = _forecastStatusImageMainFragment
    private val _cityStatusImageMainFragment = MutableLiveData<Boolean>()
    val cityStatusImageMainFragment: LiveData<Boolean>
        get() = _cityStatusImageMainFragment

    private var _getForecastResult: ForecastResponse = ForecastResponse()
    val getForecastResult: ForecastResponse
        get() = _getForecastResult
    private var _selectedCity: City = City()
    val selectedCity: City
        get() = _selectedCity
    private var _weekForecast =
        MutableLiveData<List<DayForecast>>(listOf())//mb should create another livedata just for ui forecast
    val weekForecast: LiveData<List<DayForecast>>
        get() = _weekForecast
    private var _currentWeather = MutableLiveData<CurrentWeather?>(null)
    val currentWeather: LiveData<CurrentWeather?>
        get() = _currentWeather
    private var _citySearchResult = listOf<City>()
    val citySearchResult: List<City> get() = _citySearchResult

    private lateinit var _currentLocation: Location
    val currentLocation: Location
        get() = _currentLocation

    private var latitude: Double? = null
    private var longitude: Double? = null
    private val timezone = "auto"
    private var hourly = "pressure_msl,relativehumidity_2m"
    private var daily =
        "weathercode,temperature_2m_min,temperature_2m_max,windspeed_10m_max,winddirection_10m_dominant"
    lateinit var weatherCodeMap: Map<Int, String>

    val emptyCity = City()

    /* try to do with just weekforecast livedata for now
        private var _todayForecastUi = MutableLiveData<String>()
        val todayForecastUi: LiveData<String>
            get() = _todayForecastUi
    */
    /*
    private var _weekForecastUi = MutableLiveData<List<DayForecast>>()
    val weekForecastUi: MutableLiveData<List<DayForecast>>
        get() = _weekForecastUi
     */

    private var _appUiState = MutableLiveData(AppUiStates.NORMAL)
    val appUiState: LiveData<AppUiStates> get() = _appUiState

    enum class AppUiStates {
        NORMAL, /*WAITING_GEO, WAITING_CITY_SEARCH, WAITING_FORECAST_RESPONSE,/*using boolean work flags to prevent parallel func exec instead of these*/*/
        GEO_PERM_REQUIRED, GEO_PERM_RATIONALE, NO_GEO, GEO_DETECT_FAILED, NO_INTERNET,
        CONNECTION_TIMEOUT, CITY_NOT_FOUND, /*UNEXPECTED_MISTAKE, /*DEBUG FEATURE*/*/ GO_TO_CITY_FRAGMENT,
        EMPTY_CITY_TEXT_FIELD, CHECK_LOC_PERM, API_ERROR /*, LAT_OR_LONG_NULL/*DEBUG FEATURE*/*/
    }

    enum class GetLocationByGpsErrors {
        NO_ERROR, GPS_IS_OFF, LOC_DETECTION_FAILED, MISSING_PERMISSION
    }

    enum class TimeoutJobCancelReasons {
        NOT_CANCELLED, ON_LOC_CHANGED, ON_PROVIDER_DISABLED
    }

    private var _selectCityButtonWork = false
    val selectCityButtonWork: Boolean get() = _selectCityButtonWork
    fun setSelectCityButtonWork(boolean: Boolean) {
        _selectCityButtonWork = boolean
    }

    private var _showForecastButtonWork = false
    val showForecastButtonWork: Boolean get() = _showForecastButtonWork
    fun setShowForecastButtonWork(boolean: Boolean) {
        _showForecastButtonWork = boolean
    }

    fun setLocation(location: Location) {
        _currentLocation = location
        latitude = _currentLocation.latitude
        longitude = _currentLocation.longitude
    }

    suspend fun getCitiesByName(query: String, lang: String): Pair<Boolean, String> {
        Timber.d("entered getcitiesbyname")
        var listResult = CityResponse()
        var exception: String = Constants.EMPTY_EXCEPTION
        val getCitiesJob = viewModelScope.launch {
            _citySearchResult = listOf()
            try {
                listResult =
                    OpenMeteoApi.retrofitCityService.getCityResponse(name = query, language = lang)
            } catch (e: Exception) {
                Timber.d("getCitiesByName: $e")
                exception = e.toString()
            } //finally {setSpinnerVisibilityCityFragment(false)}
        }
        getCitiesJob.join()
        Timber.d("getcitiesbyname before return")
        return if (listResult.results.isNotEmpty()) {
            _citySearchResult = listResult.results
            Pair(true, exception)
        } else {
            Pair(false, exception)
        }
    }

    @SuppressLint("SimpleDateFormat")
    suspend fun getForecastByCoords(cityLatitude: Double, cityLongitude: Double): String {
        setForecastSpinnerVisibilityMainFragment(true)
        var exception = Constants.EMPTY_EXCEPTION
        viewModelScope.launch {
            val currentDate: String = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val weekLaterDate: String = SimpleDateFormat("yyyy-MM-dd").format(
                Calendar.getInstance().timeInMillis + Constants.WEEK_IN_MILLIS
            )
            Timber.d("entered getForecastByCoords, before retrofitForecastService.getforecastresponse")
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
                Timber.d("getForecastByCoords, $e")
                exception = e.toString()
            } finally {
                Timber.d("after retrofitForecastServicegetforecastresp")
                setForecastSpinnerVisibilityMainFragment(false)
            }
        }.join()
        Timber.d("after getforecastresponse")
        return exception
    }


    fun setSelectedCity(city: City) {
        _selectedCity = city
    }

    fun resetSelectedCity() {
        _selectedCity = emptyCity
    }

    fun setWeekForecast(weekForecast: List<DayForecast>) {
        _weekForecast.value = weekForecast
    }

    fun resetWeekForecast() {
        _weekForecast.value = listOf()
    }

    fun resetCurrentWeather() {
        _currentWeather.value = null
    }

    fun resetDisplayableWeatherData() {
        resetWeekForecast()
        resetCurrentWeather()
    }

    fun resetForecastResult() {
        _getForecastResult = ForecastResponse()
    }

    fun setForecastSpinnerVisibilityMainFragment(b: Boolean) { //replaced by manually setting view.visibility in mainframent
        _forecastStatusImageMainFragment.value = b
    }

    fun setCitySpinnerVisibilityMainFragment(b: Boolean) { //replaced by manually setting view.visibility in cityframent
        _cityStatusImageMainFragment.value = b
    }

    fun setNormalAppUiState() {
        _appUiState.value = AppUiStates.NORMAL
    }


    private suspend fun setLocationGetForecast(location: Location): String {
        setLocation(location)
        currentLocation.let {
            return getForecastByCoords(it.latitude, it.longitude)
        }
    }

    private fun setForecast(forecastResult: ForecastResponse) {
        Timber.d("entered setForecast, forecastResult=$forecastResult")
        val weekForecast = forecastResult.toDayForecastList()
        _currentWeather.value = forecastResult.current_weather
        if (weekForecast[0].latitude != null) { //TODO mb replace this check with something more elegant
            Timber.d("weekForecast[0].latitude != null")
            setWeekForecast(weekForecast)
        } else {
            Timber.d("weekForecast[0].latitude == null")
            _appUiState.value = AppUiStates.API_ERROR /*LAT_OR_LONG_NULL for debug*/
            resetWeekForecast()
        }
    }

    suspend fun findCity(query: String, lang: String) {
        resetForecastResult()
        val foundAnyCities = getCitiesByName(query, lang)
        if (foundAnyCities.first) {
            _appUiState.value = AppUiStates.GO_TO_CITY_FRAGMENT/*this@MainFragment.findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToCityFragment()
                )*/
        } else if (foundAnyCities.second !== Constants.EMPTY_EXCEPTION) {
            _appUiState.value = AppUiStates.CONNECTION_TIMEOUT//showConnectionTimeoutDialog()
        } else {
            _appUiState.value = AppUiStates.CITY_NOT_FOUND//showCityNotFoundDialog()
        }
    }

    //@SuppressLint("MissingPermission")
    suspend fun tryGetCurrentLocForecast() {
        Timber.d("entered tryGetCurrentLocForecast")
        setForecastSpinnerVisibilityMainFragment(true)
        lateinit var location: Location
        val locationManager: LocationManager = context
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var lastKnownLocationByGps: Location? = null
        var lastKnownLocationByNetwork: Location? = null
        try {
            lastKnownLocationByGps =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationByNetwork =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            Timber.d("tryGetCurrentLocForecast(), security exception: $e\n lastKnownLocationByGps=$lastKnownLocationByGps, lastKnownLocationByNetwork=$lastKnownLocationByNetwork")
            _appUiState.value = AppUiStates.GEO_PERM_REQUIRED
            //showGeoPermissionRequiredDialog()
            setForecastSpinnerVisibilityMainFragment(false)
            return
        } catch (e: Exception) {
            Timber.d("tryGetCurrentLocForecast(), Unexpected exception: $e\n lastKnownLocationByGps=$lastKnownLocationByGps, lastKnownLocationByNetwork=$lastKnownLocationByNetwork")
        }
        Timber.d("tryGetCurrentLocForecast() before chooseLatestLoc, lastKnownLocationByGps=$lastKnownLocationByGps, lastKnownLocationByNetwork=$lastKnownLocationByNetwork")


        //getting latest known location (from gps or network)
        val latestKnownLoc =
            chooseLatestLocation(lastKnownLocationByGps, lastKnownLocationByNetwork)

        if ((latestKnownLoc != null) &&
            ((latestKnownLoc.time) >
                    (Calendar.getInstance().timeInMillis - Constants.MAX_LOC_AGE))
        ) {
            location = latestKnownLoc
        } else { //if no fresh enough location is present, detect location
            Timber.d("line after newLocation, error assignment")
            var (newLocation, error) = getLocation(
                locationManager,
                LocationManager.NETWORK_PROVIDER,
                Constants.DETECT_GEO_TIMEOUT_CHECK_TIMES_NETWORK
            )
            Timber.d("line after getLocation(NETWORK), newLocation=$newLocation, error=$error")
            if (error == GetLocationByGpsErrors.LOC_DETECTION_FAILED) {
                Timber.d("network loc detection failed, trying to detect loc by gps")
                val getLocRes = getLocation(
                    locationManager,
                    LocationManager.GPS_PROVIDER,
                    Constants.DETECT_GEO_TIMEOUT_CHECK_TIMES_GPS
                )
                newLocation = getLocRes.first
                error = getLocRes.second
            }
            when (error) {//TODO mb replace returns/spinnervissets, or leave one
                GetLocationByGpsErrors.GPS_IS_OFF -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    _appUiState.value = AppUiStates.NO_GEO //showNoGeoDialog()
                    return
                }
                GetLocationByGpsErrors.LOC_DETECTION_FAILED -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    Timber.d("loc detection by NETWORK_PROVIDER failed, error=$error")
                    _appUiState.value =
                        AppUiStates.GEO_DETECT_FAILED //showFailedToDetectGeoDialog()
                    return
                }
                GetLocationByGpsErrors.NO_ERROR -> {
                    if (newLocation == null) {
                        Timber.d("error=$error, newLocation == null")
                        setForecastSpinnerVisibilityMainFragment(false)
                        _appUiState.value =
                            AppUiStates.GEO_DETECT_FAILED//AppUiStates.UNEXPECTED_MISTAKE
                        return // showUnexpectedMistake in debug and showFailedToDetectGeo in release
                    }
                }
                GetLocationByGpsErrors.MISSING_PERMISSION -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    Timber.d("error=$error")
                    _appUiState.value =
                        AppUiStates.GEO_PERM_REQUIRED //showGeoPermissionRequiredDialog()
                    return
                }
            }
            location = newLocation
        }
        Timber.d("before setLocationGetForecast")
        val exception = setLocationGetForecast(location)
        if (exception != Constants.EMPTY_EXCEPTION) {
            Timber.d("after setLocationGetForecast, exception=$exception")
            _appUiState.value = AppUiStates.CONNECTION_TIMEOUT //showConnectionTimeoutDialog()
        }
        setForecastSpinnerVisibilityMainFragment(false)
    }


    fun tryGetSetCurrentLocForecast() {
        viewModelScope.launch {
            setShowForecastButtonWork(true)
            tryGetCurrentLocForecast()
            if (appUiState.value == AppUiStates.NORMAL) {
                setForecast(getForecastResult)
            }
            setShowForecastButtonWork(false)
        }
    }

    fun tryGetSelCityForecast() {
        viewModelScope.launch {
            setShowForecastButtonWork(true)
            try {
                selectedCity.let {
                    if (it.latitude != null && it.longitude != null) {
                        val exception = getForecastByCoords(it.latitude, it.longitude)
                        Timber.d("SetSelCityForecast, exception = $exception")
                        if (exception != Constants.EMPTY_EXCEPTION) {
                            _appUiState.value =
                                AppUiStates.CONNECTION_TIMEOUT //showConnectionTimeoutDialog()
                        } else {
                            setForecast(getForecastResult)
                            //prepDayForecastUiText()
                        }
                    } else {
                        _appUiState.value =
                            AppUiStates.API_ERROR /*LAT_OR_LONG_NULL for debug*/
                    }
                }
            } catch (e: Exception) {
                Timber.d("$e")
            } finally {
                setShowForecastButtonWork(false)
            }
        }
    }

    fun setAppUiState(appUiState: AppUiStates) {
        _appUiState.value = appUiState
    }

    fun checkNetworkFindCity(locQuery: String, citySearchQueryLang: String) {
        viewModelScope.launch {
            if (selectCityButtonWork) return@launch
            setSelectCityButtonWork(true)
            setCitySpinnerVisibilityMainFragment(true)
            if (isNetworkAvailable(context)) {
                findCity(locQuery, citySearchQueryLang)
            } else setAppUiState(AppUiStates.NO_INTERNET)
            setCitySpinnerVisibilityMainFragment(false)
            setSelectCityButtonWork(false)
        }
    }

    fun onShowForecastButtonClicked() {
        if (showForecastButtonWork) return
        viewModelScope.launch {
            setShowForecastButtonWork(true)
            //viewModel.setSpinnerVisibilityMainFragment(true)
            if (!isNetworkAvailable(context)) {
                setAppUiState(AppUiStates.NO_INTERNET)
                //viewModel.setSpinnerVisibilityMainFragment(false)
                setShowForecastButtonWork(false)
            } else {
                if (selectedCity == emptyCity) {
                    setAppUiState(AppUiStates.CHECK_LOC_PERM)
                    setNormalAppUiState()
                    setShowForecastButtonWork(false)
                    //checkPermDetectLoc(viewModel.requestLocPermissionLauncher)
                } else /*MainViewModel.LocSetOptions.SELECT*/ {
                    if (selectedCity.name == null) { //TODO check if this branch will ever exec
                        setAppUiState(AppUiStates.EMPTY_CITY_TEXT_FIELD)
                        setNormalAppUiState()
                        setShowForecastButtonWork(false)
                    } else {
                        tryGetSelCityForecast()
                    }
                    //viewModel.setSpinnerVisibilityMainFragment(false)
                }
                /* moving to both when branches, cause had to somehow wait for activitylauncher,
            mb should move to distinct function and pass it as lambda in higher order fun or just call it or smth
            setForecast(viewModel.getForecastResult.value)
            showDayForecast()
            viewModel.setSpinnerVisibilityMainFragment(false)*/
            }
        }
    }

    fun isNetworkAvailable(context: Context?): Boolean { //returns true if connected to wifi without internet
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else @Suppress("DEPRECATION") {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }


    //@SuppressLint("MissingPermission")
    suspend fun getLocation(
        locationManager: LocationManager,
        provider: String,
        timeoutCheckTimes: Int
    ): Pair<Location?, GetLocationByGpsErrors> {
        Timber.d("entered getLocation()")
        var error = GetLocationByGpsErrors.NO_ERROR
        var timeoutJobCancelReason = TimeoutJobCancelReasons.NOT_CANCELLED // mb unnecessary
        var location: Location? = null
        var timeOutJob: Job? = null

        //TODO mb move gpsLocationListener inside "if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))"
        val gpsLocationListener: LocationListener =
            object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    Timber.d(
                        "entered onLocationChanged, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    locationManager.removeUpdates(this)
                    Timber.d(
                        "onLocationChanged, error=$error,location=$location newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    Timber.d(
                        "onlocationchanged, line before location assign. newLocation =$newLocation, location=$location, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    location = newLocation
                    Timber.d(
                        "onlocationchanged, line after location assign. newLocation =$newLocation, location=$location, error=$error, location.time=${location?.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_LOC_CHANGED
                    Timber.d(
                        "onlocchanged, line before timeoutjob.cancel, timeOutJob=$timeOutJob"
                    )
                    timeOutJob?.cancel()
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    Timber.d("entered onproviderdisabled, error=$error")
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_PROVIDER_DISABLED
                    Timber.d(
                        "onproviderdisabled, line before timeoutjob.cancel, timeOutJob=$timeOutJob"
                    )
                    timeOutJob?.cancel()
                }

                @Deprecated(
                    "Deprecated in Java", ReplaceWith(
                        "super.onStatusChanged(provider, status, extras)",
                        "android.location.LocationListener"
                    )
                )
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    //DON'T REMOVE, will cause crash on android 5
                }

                override fun onProviderEnabled(provider: String) {
                    //DON'T REMOVE, will cause crash on android 5
                }
            }
        if (locationManager
                .isProviderEnabled(provider)
        ) {
            Timber.d("line before requesting loc updates, error=$error")
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    Constants.REQUEST_LOCATION_UPDATES_MIN_TIME_MS,
                    Constants.REQUEST_LOCATION_UPDATES_MIN_DISTANCE_M,
                    gpsLocationListener
                )
            } catch (e: SecurityException) {
                Timber.d("Security exception: $e")
                return Pair(null, GetLocationByGpsErrors.MISSING_PERMISSION)
            } catch (e: Exception) {
                Timber.d("Unexpected exception: ${e})")
            }
            Timber.d("line before launching waiter, error=$error")
            timeOutJob =
                viewModelScope.launch {//TODO mb change to out of the box withTimeout()
                    repeat(timeoutCheckTimes) {
                        if (isActive) {
                            delay(Constants.DETECT_GEO_TIMEOUT_CHECK_PERIOD_IN_MILLIS)
                            Timber.d("line in waiter, iter $it, error=$error")
                        } else {
                            return@launch
                        }
                    }
                    locationManager.removeUpdates(gpsLocationListener)
                }
            Timber.d(
                "line after launching waiter, before timeoutjob.join, error=$error"
            )
            timeOutJob.join()
            Timber.d("line after timeoutjob.join, error=$error")
            when (timeoutJobCancelReason) {
                TimeoutJobCancelReasons.ON_LOC_CHANGED -> {
                    Timber.d("entered timeoutJobCancelReasons.ON_LOC_CHANGED ->")
                }
                TimeoutJobCancelReasons.ON_PROVIDER_DISABLED -> {
                    Timber.d(
                        "entered timeoutJobCancelReasons.ON_PROVIDER_DISABLED ->"
                    )
                    if (provider != LocationManager.NETWORK_PROVIDER) {
                        error = GetLocationByGpsErrors.GPS_IS_OFF
                    } else {
                        error = GetLocationByGpsErrors.LOC_DETECTION_FAILED
                    }
                }
                TimeoutJobCancelReasons.NOT_CANCELLED -> {//reached timeout
/* last try, get any, even old location. mb do this after function completes
location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?:
locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
 */
                    Timber.d(
                        "location=$location, line before when (abt location), error=$error)"
                    )
                    error = GetLocationByGpsErrors.LOC_DETECTION_FAILED
                }
            }
        } else {
            if (provider != LocationManager.NETWORK_PROVIDER) {
                error = GetLocationByGpsErrors.GPS_IS_OFF
            } else {
                error = GetLocationByGpsErrors.LOC_DETECTION_FAILED
            }
        }
        Timber.d("getLocation() penultimate line, error=$error")
        return Pair(location, error)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun shouldOpenLocSettings(): Boolean {
        return try {
            !isLocationEnabled()
        } catch (_: Exception) {
            false
        }
    }

    fun weatherCodeToWords(
        dayForecast: DayForecast,
    ): DayForecast {
        return dayForecast.run {
            DayForecast(
                id, latitude, longitude, pressure, relativeHumidity,
                weatherCodeMap[weather?.toInt()], temperature2mMin, temperature2mMax,
                windspeed10mMax, winddirection10mDominant
            )
        }
    }
}