package com.example.weatherforecast.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.weatherforecast.Constants
import com.example.weatherforecast.R
import com.example.weatherforecast.data.DayForecast
import com.example.weatherforecast.databinding.FragmentMainBinding
import com.example.weatherforecast.network.ForecastResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

class MainFragment : Fragment() {
    //TODO where necessary prevent calling functions (like after clicking a button), when they are already running
    //TODO check setSpinnerVisibility placement

    /*
    companion object {
        fun newInstance() = MainFragment()
    }
    */
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var weatherCodeMap: Map<Int, String>

    enum class GetLocationByGpsErrors {
        NO_ERROR, GPS_IS_OFF, LOC_DETECTION_FAILED
    }

    enum class TimeoutJobCancelReasons {
        NOT_CANCELLED, ON_LOC_CHANGED, ON_PROVIDER_DISABLED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.requestLocPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        tryGetCurrentLocForecast()  //TODO maybe better move this call somewhere else
                        //mb unite next 3 lines into a sep fun
                        Log.d("MainFragment", "line between trygetcur and setforec")
                        setForecast(viewModel.getForecastResult.value)
                        showDayForecast()
                        viewModel.setSpinnerVisibilityMainFragment(false)
                        //viewModel.setSpinnerVisibilityMainFragment(false)
                    }
                } else {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showGeoPermissionRequiredDialog()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showForecastButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                onShowForecastButtonClicked()
            }
        }
        binding.rbCurrentCity.setOnClickListener {
            viewModel.setLocOption(MainViewModel.LocSetOptions.CURRENT)
            viewModel.resetWeekForecast()
        }
        binding.rbSelectCity.setOnClickListener {
            viewModel.setLocOption(MainViewModel.LocSetOptions.SELECT)
            viewModel.resetWeekForecast()
        }
        viewModel.locationSettingOption.observe(this.viewLifecycleOwner) { option ->
            when (option) {
                MainViewModel.LocSetOptions.CURRENT -> {
                    binding.rbCurrentCity.isChecked = true
                    binding.textField.isEnabled = false
                    binding.selectCityButton.isEnabled = false
                    viewModel.resetSelectedCity()
                    viewModel.resetWeekForecast()
                }
                MainViewModel.LocSetOptions.SELECT -> {
                    binding.rbSelectCity.isChecked = true
                    binding.textField.isEnabled = true
                    binding.selectCityButton.isEnabled = true
                    binding.selectedCityTextView.text = ""
                    viewModel.resetWeekForecast()
                }
                else -> {}
            }

        }

        binding.selectCityButton.setOnClickListener {//TODO add an error message if query textbox is empty
            viewLifecycleOwner.lifecycleScope.launch {//TODO MOVE THIS BLOCK OF CODE INTO A FUNCTION, REPLACE WITH FUN CALL
                var foundAnyCities = false
                viewModel.resetForecastResult()
                val job = viewLifecycleOwner.lifecycleScope.launch {
                    foundAnyCities =
                        viewModel.getCitiesByName(binding.textFieldInput.text.toString())
                }
                job.join()
                if (foundAnyCities) {
                    val action = MainFragmentDirections.actionMainFragmentToCityFragment()
                    this@MainFragment.findNavController().navigate(action)
                } else {
                    showCityNotFoundDialog()
                }
            }
        }

        binding.weekForecastButton.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToItemFragment()
            this.findNavController().navigate(action)
        }

        viewModel.selectedCity.observe(this.viewLifecycleOwner) { city ->
            if (city.name == null) {
                if (viewModel.locationSettingOption.value == MainViewModel.LocSetOptions.CURRENT) {
                    binding.selectedCityTextView.text = getString(
                        R.string.selected_city_text_current_location
                    )
                } else {
                    binding.selectedCityTextView.text = ""
                }
            } else city.let {
                binding.selectedCityTextView.text = viewModel.prepCityForUi(city)
            }
        }


        /*viewModel.getForecastResult.observe(this.viewLifecycleOwner) {
            setForecast(it)
        }*/
        weatherCodeMap = mapOf(
            0 to getString(R.string.wc0),
            1 to getString(R.string.wc1),
            2 to getString(R.string.wc2),
            3 to getString(R.string.wc3),
            45 to getString(R.string.wc45),
            48 to getString(R.string.wc48),
            51 to getString(R.string.wc51),
            53 to getString(R.string.wc53),
            55 to getString(R.string.wc55),
            56 to getString(R.string.wc56),
            57 to getString(R.string.wc57),
            61 to getString(R.string.wc61),
            63 to getString(R.string.wc63),
            65 to getString(R.string.wc65),
            66 to getString(R.string.wc66),
            67 to getString(R.string.wc67),
            71 to getString(R.string.wc71),
            73 to getString(R.string.wc73),
            75 to getString(R.string.wc75),
            77 to getString(R.string.wc77),
            80 to getString(R.string.wc80),
            81 to getString(R.string.wc81),
            82 to getString(R.string.wc82),
            85 to getString(R.string.wc85),
            86 to getString(R.string.wc86),
            95 to getString(R.string.wc95),
            96 to getString(R.string.wc96),
            99 to getString(R.string.wc99)
        )
        viewModel.statusImageMainFragment.observe(this.viewLifecycleOwner)
        {
            if (it) {
                binding.statusImage.visibility = View.VISIBLE
            } else {
                binding.statusImage.visibility = View.GONE
            }
        }

        /*viewModel.weekForecast.observe(this.viewLifecycleOwner) {
            if (viewModel.weekForecast.value?.isEmpty() != false) {
                binding.todayForecastTextView.text = ""
            } else {
                val todayForecast = viewModel.weekForecast.value?.get(0)
                binding.todayForecastTextView.text = getString(
                    R.string.day_forecast,
                    todayForecast?.temperature2mMin ?: "",
                    todayForecast?.temperature2mMax ?: "",
                    todayForecast?.weather ?: "",
                    todayForecast?.pressure ?: "",
                    todayForecast?.windspeed10mMax ?: "",
                    todayForecast?.winddirection10mDominant ?: "",
                    todayForecast?.relativeHumidity ?: ""
                )
            }
            binding.weekForecastButton.isEnabled =
                (viewModel.weekForecast.value?.isNotEmpty() == true)
        }*/


    }


    private fun showNoGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_dialog_title))
            .setMessage(getString(R.string.no_geo_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .show()
    }

    private fun showFailedToDetectGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.failed_to_detect_geo_dialog_title))
            .setMessage(getString(R.string.failed_to_detect_geo_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.failed_to_detect_geo_dialog_button) { _, _ -> }
            .show()
    }

    private fun showGeoPermissionRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_permission_dialog_title))
            .setMessage(getString(R.string.no_geo_permission_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_permission_dialog_button) { _, _ -> }
            .show()
    }

    private fun showCityNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.city_not_found_dialog_title))
            .setMessage(getString(R.string.city_not_found_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .show()
    }

    private fun showUnexpectedMistake() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.unexpected_mistake_text))
            .setMessage(getString(R.string.unexpected_mistake_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .show()
    }

    private fun showGeoPermissionRationaleDialog(activityResultLauncher: ActivityResultLauncher<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_permission_dialog_title))
            .setMessage(
                getString(
                    R.string.location_permission_rationale_message,
                    getString(R.string.current_city_rb)
                )
            )
            .setCancelable(true)
            .setPositiveButton(getString(R.string.location_permission_rationale_pos_button)) { _, _ ->
                activityResultLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            .setNegativeButton(R.string.location_permission_rationale_neg_button) { _, _ ->
                viewModel.setSpinnerVisibilityMainFragment(false)
            }
            .show()
    }

    private suspend fun checkPermDetectLoc(activityResultLauncher: ActivityResultLauncher<String>) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                tryGetCurrentLocForecast()
                setForecast(viewModel.getForecastResult.value)
                showDayForecast()
                viewModel.setSpinnerVisibilityMainFragment(false)

                //viewModel.setSpinnerVisibilityMainFragment(false)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                //viewModel.setSpinnerVisibilityMainFragment(false)
                showGeoPermissionRationaleDialog(activityResultLauncher)
            }
            else -> {
                activityResultLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
    }

    suspend fun setLocationGetForecast(location: Location) {
        viewModel.setLocation(location)
        viewModel.currentLocation.let {
            viewModel.getForecastByCoords(it.latitude, it.longitude)
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
            val humanWeather: String =
                weatherCodeMap[forecastResponse.daily.weathercode[it]] ?: ""
            weekForecast.add(
                DayForecast(
                    latitude = forecastResponse.latitude,
                    longitude = forecastResponse.longitude,
                    pressure = dayPressure.toString() + getString(R.string.pressure_unit),
                    relativeHumidity = dayRelativeHumidity.toString() + getString(R.string.relative_humidity_unit),
                    weather = humanWeather,
                    temperature2mMin = forecastResponse.daily.temperature_2m_min[it].toString() + getString(
                        R.string.temperature_unit
                    ),
                    temperature2mMax = forecastResponse.daily.temperature_2m_max[it].toString() + getString(
                        R.string.temperature_unit
                    ),
                    windspeed10mMax = forecastResponse.daily.windspeed_10m_max[it].toString() + getString(
                        R.string.wind_speed_unit
                    ),
                    winddirection10mDominant = forecastResponse.daily.winddirection_10m_dominant[it].toString() + getString(
                        R.string.wind_direction_unit
                    ),
                    timeStamp = Calendar.getInstance().timeInMillis
                )
            )
        }
        return weekForecast
    }

    @SuppressLint("MissingPermission")
    suspend fun tryGetCurrentLocForecast() {//TODO add permission exception handling
        lateinit var location: Location
        val locationManager: LocationManager = context
            ?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocationByGps =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastKnownLocationByNetwork =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
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
            Log.d("MainFragment", "string after newLocation, error assignment")
            when (error) {//TODO mb replace returns/spinnervissets, or leave one
                GetLocationByGpsErrors.GPS_IS_OFF -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showNoGeoDialog(); return
                }
                GetLocationByGpsErrors.LOC_DETECTION_FAILED -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    Log.d("MainFragment", "error=$error")
                    showFailedToDetectGeoDialog(); return
                }
                GetLocationByGpsErrors.NO_ERROR -> {
                    if (newLocation == null) {
                        Log.d("MainFragment", "error=$error, newLocation == null")
                        viewModel.setSpinnerVisibilityMainFragment(false)
                        showUnexpectedMistake(); return //TODO change showUnexpectedMistake showFailedToDetectGeo in release
                    }
                }
            }
            location = newLocation
        }
        setLocationGetForecast(location)
        //viewModel.setSpinnerVisibilityMainFragment(false)
    }

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

    @SuppressLint("MissingPermission")
    suspend fun getLocationByGps(locationManager: LocationManager): Pair<Location?, GetLocationByGpsErrors> {
        //TODO add permission exception handling (mb just in parent func)
        Log.d("MainFragment", "entered getLocationByGps")
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
                        "MainFragment",
                        "entered onLocationChanged, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    Log.d(
                        "MainFragment",
                        "onlocationchanged, string before location assign. newLocation =$newLocation, location=$location, error=$error, newLocation.time=${newLocation.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    location = newLocation
                    Log.d(
                        "MainFragment",
                        "onlocationchanged, string after location assign. newLocation =$newLocation, location=$location, error=$error, location.time=${location?.time}, calendar.getinstance.timeinmillis=${Calendar.getInstance().timeInMillis}"
                    )
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_LOC_CHANGED
                    Log.d(
                        "MainFragment",
                        "onlocchanged, string before timeoutjob.cancel, timeOutJob=$timeOutJob"
                    )
                    timeOutJob?.cancel()
                }

                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                    locationManager.removeUpdates(this)

                    Log.d("MainFragment", "entered onproviderdisabled, error=$error")
                    timeoutJobCancelReason = TimeoutJobCancelReasons.ON_PROVIDER_DISABLED
                    Log.d(
                        "MainFragment",
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
            Log.d("MainFragment", "string before requesting loc updates, error=$error")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                Constants.REQUEST_LOCATION_UPDATES_MIN_TIME_MS,
                Constants.REQUEST_LOCATION_UPDATES_MIN_DISTANCE_M,
                gpsLocationListener
            )
            Log.d("MainFragment", "string before launching waiter, error=$error")
            timeOutJob =
                viewLifecycleOwner.lifecycleScope.launch {//TODO mb change to out of the box withTimeout()
                    repeat(Constants.DETECT_GEO_TIMEOUT_CHECK_TIMES) {
                        if (isActive) {
                            delay(Constants.DETECT_GEO_TIMEOUT_CHECK_PERIOD_IN_MILLIS)
                            Log.d("MainFragment", "string in waiter, iter $it, error=$error")
                        } else {
                            return@launch
                        }
                    }
                    locationManager.removeUpdates(gpsLocationListener)
                }
            Log.d(
                "MainFragment",
                "string after launching waiter, before timeoutjob.join, error=$error"
            )
            timeOutJob.join()
            Log.d("MainFragment", "string after timeoutjob.join, error=$error")
            when (timeoutJobCancelReason) {
                TimeoutJobCancelReasons.ON_LOC_CHANGED -> {
                    Log.d("MainFragment", "entered timeoutJobCancelReasons.ON_LOC_CHANGED ->")
                }
                TimeoutJobCancelReasons.ON_PROVIDER_DISABLED -> {
                    Log.d("MainFragment", "entered timeoutJobCancelReasons.ON_PROVIDER_DISABLED ->")
                    error = GetLocationByGpsErrors.GPS_IS_OFF
                }
                TimeoutJobCancelReasons.NOT_CANCELLED -> {//reached timeout
/* last try, get any, even old location. mb do this after function completes
location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?:
locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
 */
                    Log.d(
                        "MainFragment",
                        "location=$location, string before when (abt location), error=$error)"
                    )
                    error = GetLocationByGpsErrors.LOC_DETECTION_FAILED
                }
            }
        } else {
            error = GetLocationByGpsErrors.GPS_IS_OFF
//showNoGeoDialog()
        }
        Log.d("MainFragment", "getLocationByGps penultimate string, error=$error")
        return Pair(location, error)
    }

    suspend fun onShowForecastButtonClicked() {
        viewModel.setSpinnerVisibilityMainFragment(true)
        when (viewModel.locationSettingOption.value) {
            MainViewModel.LocSetOptions.CURRENT -> {
                //viewModel.setSpinnerVisibilityMainFragment(true)
                checkPermDetectLoc(viewModel.requestLocPermissionLauncher)
            }
            MainViewModel.LocSetOptions.SELECT -> {
                if (viewModel.selectedCity.value?.name == null) {
                    Snackbar.make(
                        binding.showForecastButton,
                        getString(R.string.select_city_snackbar),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                } else {
                    viewModel.selectedCity.value.let {
                        it?.latitude?.let { it1 ->
                            it.longitude?.let { it2 ->
                                //viewModel.setSpinnerVisibilityMainFragment(true)
                                viewModel.getForecastByCoords(it1, it2)
                            }
                        }
                    }
                    setForecast(viewModel.getForecastResult.value)
                    showDayForecast()
                    viewModel.setSpinnerVisibilityMainFragment(false)
                }
            }
            else -> {} //shouldn't be necessary after replacing livedata with a normal variable
        }
        /* moving to both when branches, cause had to somehow wait for activitylauncher,
        mb should move to distinct function and pass it as lambda in higher order fun or just call it or smth
        setForecast(viewModel.getForecastResult.value)
        showDayForecast()
        viewModel.setSpinnerVisibilityMainFragment(false)*/
    }

    fun setForecast(forecastResult: ForecastResponse?) {
        Log.d("MainFragment", "entered setForecast, forecastResult=$forecastResult")
        if (forecastResult != null) {
            val weekForecast = handleForecastResponse(forecastResult)
            if (weekForecast[0].latitude != null) { //TODO replace this check with something more elegant
                viewModel.setWeekForecast(weekForecast)
            } else {
                viewModel.resetWeekForecast()
            }
        }
    }

    fun showDayForecast() {
        if (viewModel.weekForecast.value?.isEmpty() != false) {
            binding.todayForecastTextView.text = ""
        } else {
            val todayForecast = viewModel.weekForecast.value?.get(0)
            binding.todayForecastTextView.text = getString(
                R.string.day_forecast,
                todayForecast?.temperature2mMin ?: "",
                todayForecast?.temperature2mMax ?: "",
                todayForecast?.weather ?: "",
                todayForecast?.pressure ?: "",
                todayForecast?.windspeed10mMax ?: "",
                todayForecast?.winddirection10mDominant ?: "",
                todayForecast?.relativeHumidity ?: ""
            )
        }
        binding.weekForecastButton.isEnabled =
            (viewModel.weekForecast.value?.isNotEmpty() == true)
    }
}