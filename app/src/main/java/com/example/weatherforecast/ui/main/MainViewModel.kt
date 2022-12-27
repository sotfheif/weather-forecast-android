package com.example.weatherforecast.ui.main
//TODO CHECK thar forecast livedata for UI is set to null in the beginning of some funs

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.*
import com.example.weatherforecast.Constants
import com.example.weatherforecast.data.DayForecast
import com.example.weatherforecast.network.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    lateinit var requestLocPermissionLauncher: ActivityResultLauncher<String>

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
        NORMAL, WAITING_GEO, WAITING_CITY_SEARCH, WAITING_FORECAST_RESPONSE,
        GEO_PERM_REQUIRED, GEO_PERM_RATIONALE, NO_GEO, GEO_DETECT_FAILED, NO_INTERNET,
        CONNECTION_TIMEOUT, CITY_NOT_FOUND, UNEXPECTED_MISTAKE, GO_TO_CITY_FRAGMENT,
        EMPTY_CITY_TEXT_FIELD, CHECK_LOC_PERM, LAT_OR_LONG_NULL/*TODO remove in release. debug value*/
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

    suspend fun getCitiesByName(query: String): Pair<Boolean, String> {
        Log.d(TAG, "entered getcitiesbyname")
        var listResult = CityResponse()
        var exception: String = Constants.emptyException
        val getCitiesJob = viewModelScope.launch {
            _citySearchResult = listOf()
            try {
                listResult = OpenMeteoApi.retrofitCityService.getCityResponse(name = query)
            } catch (e: Exception) {
                Log.d("getCitiesByName", e.toString())
                exception = e.toString()
            } //finally {setSpinnerVisibilityCityFragment(false)}
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
        setForecastSpinnerVisibilityMainFragment(true)
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
                setForecastSpinnerVisibilityMainFragment(false)
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

    fun setWeekForecast(weekForecast: List<DayForecast>) {
        _weekForecast.value = weekForecast
    }

    fun resetWeekForecast() {
        _weekForecast.value = listOf()
    }

    fun resetForecastResult() {
        _getForecastResult = ForecastResponse()
    }

    fun setForecastSpinnerVisibilityMainFragment(b: Boolean) { //replaced by manually setting view.visibility in mainframent
        _forecastStatusImageMainFragment.value = b
    }

    private fun setCitySpinnerVisibilityMainFragment(b: Boolean) { //replaced by manually setting view.visibility in cityframent
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
        Log.d(TAG, "entered setForecast, forecastResult=$forecastResult")
        val weekForecast = handleForecastResponse(forecastResult)
        if (weekForecast[0].latitude != null) { //TODO mb replace this check with something more elegant
            Log.d(TAG, "weekForecast[0].latitude != null")
            setWeekForecast(weekForecast)
        } else {
            Log.d(TAG, "weekForecast[0].latitude == null")
            _appUiState.value = AppUiStates.LAT_OR_LONG_NULL
            resetWeekForecast()
        }
    }

    fun findCity(query: String) {
        viewModelScope.launch {
            setSelectCityButtonWork(true)
            setCitySpinnerVisibilityMainFragment(true)
            var foundAnyCities = Pair(false, Constants.emptyException)
            resetForecastResult()
            foundAnyCities = getCitiesByName(query)
            setCitySpinnerVisibilityMainFragment(false)
            if (foundAnyCities.first) {
                _appUiState.value = AppUiStates.GO_TO_CITY_FRAGMENT/*this@MainFragment.findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToCityFragment()
                )*/
            } else if (foundAnyCities.second !== Constants.emptyException) {
                _appUiState.value = AppUiStates.CONNECTION_TIMEOUT//showConnectionTimeoutDialog()
            } else {
                _appUiState.value = AppUiStates.CITY_NOT_FOUND//showCityNotFoundDialog()
            }
            setSelectCityButtonWork(false)
        }
    }

    private fun handleForecastResponse(forecastResponse: ForecastResponse):
            List<DayForecast> {
        if (forecastResponse.latitude == null) {
            return mutableListOf(DayForecast())
        }
        val weekForecast: MutableList<DayForecast> = mutableListOf()
        repeat(7) {
            val dayPressure = round(
                forecastResponse.hourly.pressure_msl
                    .subList(it * 24, (it + 1) * 24).average() * 10
            ) / 10
            val dayRelativeHumidity = forecastResponse.hourly.relativehumidity_2m
                .subList(it * 24, (it + 1) * 24).average().roundToInt()
            weekForecast.add(
                DayForecast(
                    latitude = forecastResponse.latitude,
                    longitude = forecastResponse.longitude,
                    pressure = dayPressure.toString(),
                    relativeHumidity = dayRelativeHumidity.toString(),
                    weather = forecastResponse.daily.weathercode[it].toString(),
                    temperature2mMin = forecastResponse.daily.temperature_2m_min[it].toString(),
                    temperature2mMax = forecastResponse.daily.temperature_2m_max[it].toString(),
                    windspeed10mMax = forecastResponse.daily.windspeed_10m_max[it].toString(),
                    winddirection10mDominant = forecastResponse.daily
                        .winddirection_10m_dominant[it].toString(),
                    timeStamp = Calendar.getInstance().timeInMillis
                )
            )
        }
        return weekForecast
    }

    //@SuppressLint("MissingPermission")
    suspend fun tryGetCurrentLocForecast() {
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
            Log.d(TAG, "Security exception: $e")
            _appUiState.value = AppUiStates.GEO_PERM_REQUIRED
            //showGeoPermissionRequiredDialog()
            setForecastSpinnerVisibilityMainFragment(false)
            return
        } catch (e: Exception) {
            Log.d(TAG, "Unexpected exception: ${e})")
        }
        //getting latest known location (from gps or network)
        val latestKnownLoc =
            chooseLatestLocation(lastKnownLocationByGps, lastKnownLocationByNetwork)

        if ((latestKnownLoc != null) &&
            ((latestKnownLoc.time) >
                    (Calendar.getInstance().timeInMillis - Constants.MAX_LOC_AGE))
        ) {
            location = latestKnownLoc
        } else { //if no fresh enough location is present, detect location
            val (newLocation, error) = getLocationByGps(locationManager)
            Log.d(TAG, "string after newLocation, error assignment")
            when (error) {//TODO mb replace returns/spinnervissets, or leave one
                GetLocationByGpsErrors.GPS_IS_OFF -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    _appUiState.value = AppUiStates.NO_GEO //showNoGeoDialog()
                    return
                }
                GetLocationByGpsErrors.LOC_DETECTION_FAILED -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    Log.d(TAG, "error=$error")
                    _appUiState.value =
                        AppUiStates.GEO_DETECT_FAILED //showFailedToDetectGeoDialog()
                    return
                }
                GetLocationByGpsErrors.NO_ERROR -> {
                    if (newLocation == null) {
                        Log.d(TAG, "error=$error, newLocation == null")
                        setForecastSpinnerVisibilityMainFragment(false)
                        _appUiState.value = AppUiStates.UNEXPECTED_MISTAKE //showUnexpectedMistake()
                        return //TODO change showUnexpectedMistake to showFailedToDetectGeo in release
                    }
                }
                GetLocationByGpsErrors.MISSING_PERMISSION -> {
                    setForecastSpinnerVisibilityMainFragment(false)
                    Log.d(TAG, "error=$error")
                    _appUiState.value =
                        AppUiStates.GEO_PERM_REQUIRED //showGeoPermissionRequiredDialog()
                    return
                }
            }
            location = newLocation
        }
        val exception = setLocationGetForecast(location)
        if (exception != Constants.emptyException) {
            Log.d(TAG, exception)
            _appUiState.value = AppUiStates.CONNECTION_TIMEOUT //showConnectionTimeoutDialog()
        }
        setForecastSpinnerVisibilityMainFragment(false)
    }

    private fun chooseLatestLocation(
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
                        if (exception != Constants.emptyException) {
                            _appUiState.value =
                                AppUiStates.CONNECTION_TIMEOUT //showConnectionTimeoutDialog()
                        } else {
                            setForecast(getForecastResult)
                            //prepDayForecastUiText()
                        }
                    } else {
                        _appUiState.value =
                            AppUiStates.LAT_OR_LONG_NULL/*showLatOrLongNullDialog()*/
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "$e")
            } finally {
                setShowForecastButtonWork(false)
            }
        }
    }

    fun setAppUiState(appUiState: AppUiStates) {
        _appUiState.value = appUiState
    }

    fun checkNetworkFindCity(locQuery: String) {
        if (isNetworkAvailable(context)) {
            findCity(locQuery)
        } else setAppUiState(AppUiStates.NO_INTERNET)
    }

    fun onShowForecastButtonClicked() {
        if (showForecastButtonWork) return
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
    suspend fun getLocationByGps(locationManager: LocationManager): Pair<Location?, GetLocationByGpsErrors> {
        Log.d(TAG, "entered getLocationByGps")
        var error = GetLocationByGpsErrors.NO_ERROR
        var timeoutJobCancelReason = TimeoutJobCancelReasons.NOT_CANCELLED // mb unnecessary
        var location: Location? = null
        var timeOutJob: Job? = null

        //TODO mb move gpsLocationListener inside "if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))"
        val gpsLocationListener: LocationListener =
            object : LocationListener {
                override fun onLocationChanged(newLocation: Location) {
                    locationManager.removeUpdates(this)
                    Log.d(
                        TAG,
                        "entered onLocationChanged, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    Log.d(
                        TAG,
                        "onlocationchanged, string before location assign. newLocation =$newLocation, location=$location, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    location = newLocation
                    Log.d(
                        TAG,
                        "onlocationchanged, string after location assign. newLocation =$newLocation, location=$location, error=$error, location.time=${location?.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_LOC_CHANGED
                    Log.d(
                        TAG,
                        "onlocchanged, string before timeoutjob.cancel, timeOutJob=$timeOutJob"
                    )
                    timeOutJob?.cancel()
                }

                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                    locationManager.removeUpdates(this)

                    Log.d(TAG, "entered onproviderdisabled, error=$error")
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_PROVIDER_DISABLED
                    Log.d(
                        TAG,
                        "onproviderdisabled, string before timeoutjob.cancel, timeOutJob=$timeOutJob"
                    )
                    //error = GetLocationByGpsErrors.GPS_IS_OFF
                    timeOutJob?.cancel()
                    //showNoGeoDialog()
                }
            }
        if (locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
        ) {
            Log.d(TAG, "string before requesting loc updates, error=$error")
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    Constants.REQUEST_LOCATION_UPDATES_MIN_TIME_MS,
                    Constants.REQUEST_LOCATION_UPDATES_MIN_DISTANCE_M,
                    gpsLocationListener
                )
            } catch (e: SecurityException) {
                Log.d(TAG, "Security exception: $e")
                return Pair(null, GetLocationByGpsErrors.MISSING_PERMISSION)
            } catch (e: Exception) {
                Log.d(TAG, "Unexpected exception: ${e})")
            }
            Log.d(TAG, "string before launching waiter, error=$error")
            timeOutJob =
                viewModelScope.launch {//TODO mb change to out of the box withTimeout()
                    repeat(Constants.DETECT_GEO_TIMEOUT_CHECK_TIMES) {
                        if (isActive) {
                            delay(Constants.DETECT_GEO_TIMEOUT_CHECK_PERIOD_IN_MILLIS)
                            Log.d(TAG, "string in waiter, iter $it, error=$error")
                        } else {
                            return@launch
                        }
                    }
                    locationManager.removeUpdates(gpsLocationListener)
                }
            Log.d(
                TAG,
                "string after launching waiter, before timeoutjob.join, error=$error"
            )
            timeOutJob.join()
            Log.d(TAG, "string after timeoutjob.join, error=$error")
            when (timeoutJobCancelReason) {
                TimeoutJobCancelReasons.ON_LOC_CHANGED -> {
                    Log.d(TAG, "entered timeoutJobCancelReasons.ON_LOC_CHANGED ->")
                }
                TimeoutJobCancelReasons.ON_PROVIDER_DISABLED -> {
                    Log.d(TAG, "entered timeoutJobCancelReasons.ON_PROVIDER_DISABLED ->")
                    error = GetLocationByGpsErrors.GPS_IS_OFF
                }
                TimeoutJobCancelReasons.NOT_CANCELLED -> {//reached timeout
/* last try, get any, even old location. mb do this after function completes
location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?:
locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
 */
                    Log.d(
                        TAG,
                        "location=$location, string before when (abt location), error=$error)"
                    )
                    error = GetLocationByGpsErrors.LOC_DETECTION_FAILED
                }
            }
        } else {
            error = GetLocationByGpsErrors.GPS_IS_OFF
//showNoGeoDialog()
        }
        Log.d(TAG, "getLocationByGps penultimate string, error=$error")
        return Pair(location, error)
    }
}