package com.example.weatherforecast.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

private const val TAG = "MainFragment"
class MainFragment : Fragment() {
    //TODO test connection unavailable
    //TODO bug sometimes forecast won't appear in ui. like when internet speed is low, you first press show forecast, but deny the perm, then enter and select some city and press showforecast after clicking show forecst once more it appears
    //TODO where necessary prevent calling functions (like after clicking a button), when they are already running. or stop some functions after conflicting functions are called
    //TODO check setSpinnerVisibility placement. sometimes mainfragment showforecast spinner won't dissapear (when rotating device during loading
    //TODO later review architecture
    //TODO later download web service's location db (update regularly in background), and make search with spinner so that possible options are shown and updated after every char entered/deleted
    //TODO later in search field (before any chars entered) show previous location search queries(or selected results(locations)?
    /*
    companion object {
        fun newInstance() = MainFragment()
    }
    */
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var weatherCodeMap: Map<Int, String>

    enum class GetLocationByGpsErrors {
        NO_ERROR, GPS_IS_OFF, LOC_DETECTION_FAILED, MISSING_PERMISSION
    }

    enum class TimeoutJobCancelReasons {
        NOT_CANCELLED, ON_LOC_CHANGED, ON_PROVIDER_DISABLED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        viewModel.requestLocPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (!isNetworkAvailable(activity?.applicationContext)) {
                            showNoInternetDialog()
                        } else {
                            tryGetCurrentLocForecast()  //TODO maybe better move this call somewhere else
                            //mb unite next 3 lines into a sep fun
                            setForecast(viewModel.getForecastResult)
                            showDayForecast()
                        }
                        viewModel.setSpinnerVisibilityMainFragment(false)
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
        Log.d(TAG, "onCreateView")
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        binding.showForecastButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                onShowForecastButtonClicked()
            }
        }
        binding.currentLocationButton.setOnClickListener {
            Log.d(TAG, "CurrentLocationButton onclicklistener")
            if (viewModel.selectedCity == viewModel.emptyCity) {
                return@setOnClickListener
            }
            viewModel.resetWeekForecast()
            binding.weekForecastButton.isEnabled =
                (viewModel.weekForecast.isNotEmpty())
            binding.todayForecastTextView.text = ""
            viewModel.resetSelectedCity()
            binding.selectedCityTextView.text =
                getString(R.string.selected_city_text_current_location)
            Log.d(
                TAG,
                "set selectedCityTextView.text = ${getString(R.string.selected_city_text_current_location)}"
            )
        }
        /*
        binding.rbSelectCity.setOnClickListener {

            Log.d(TAG, "rbSelectCity onclicklistener")
            if(viewModel.locationSettingOption.value==MainViewModel.LocSetOptions.SELECT){
                return@setOnClickListener
            }
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
         */

        binding.selectCityButton.setOnClickListener {//TODO  mb extract THIS BLOCK OF CODE INTO A FUNCTION
            if (binding.textFieldInput.text.toString().isBlank()) {
                Snackbar.make(
                    binding.showForecastButton,
                    getString(R.string.enter_city_snackbar),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            } else if (isNetworkAvailable(activity?.applicationContext)) {
                var foundAnyCities = Pair(false, Constants.emptyException)
                viewModel.resetForecastResult()
                viewLifecycleOwner.lifecycleScope.launch {
                    val job = viewLifecycleOwner.lifecycleScope.launch {
                        foundAnyCities =
                            viewModel.getCitiesByName(binding.textFieldInput.text.toString())
                    }
                    job.join()
                    if (foundAnyCities.first) {
                        this@MainFragment.findNavController().navigate(
                            MainFragmentDirections.actionMainFragmentToCityFragment()
                        )
                    } else if (foundAnyCities.second !== Constants.emptyException) {
                        showConnectionTimeoutDialog()
                    } else {
                        showCityNotFoundDialog()
                    }
                }
            } else showNoInternetDialog()
        }

        binding.weekForecastButton.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToItemFragment()
            this.findNavController().navigate(action)
        }
/*
        viewModel.selectedCity.observe(this.viewLifecycleOwner) { city ->
            if (city.name == null) {
                if (viewModel.locationSettingOption.value == MainViewModel.LocSetOptions.CURRENT) {
                    binding.selectedCityTextView.text = getString(
                        R.string.selected_city_text_current_location
                    )
                } else {
                    binding.selectedCityTextView.text = ""
                }
            } else  {
                binding.selectedCityTextView.text = viewModel.prepCityForUi(city)
            }
        }*/


        /*viewModel.getForecastResult.observe(this.viewLifecycleOwner) {
            setForecast(it)
        }*/

        weatherCodeMap = mapOf(/*mb do something with this*/
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

    /*
        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            Log.d(TAG, "onSaveInstanceState")
        }

        override fun onStart() {
            super.onStart()
            Log.d(TAG, "onStart")
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "onPause")
        }

        override fun onResume() {
            super.onResume()
            Log.d(TAG, "onResume")
        }

        override fun onStop() {
            super.onStop()
            Log.d(TAG, "onStop")
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "onDestroy")
        }

        override fun onDestroyView() {
            super.onDestroyView()
            Log.d(TAG, "onDestroyView")
        }
    */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.d(TAG, "onViewStateRestored")
        showDayForecast()
        viewModel.selectedCity.let {
            binding.selectedCityTextView.text = if (it == viewModel.emptyCity)
                getString(R.string.selected_city_text_current_location) else viewModel.prepCityForUi(
                it
            )
        }
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

    private fun showNoInternetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_internet_dialog_title))
            .setMessage(getString(R.string.no_internet_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_internet_dialog_button) { _, _ -> }
            .show()
    }

    private fun showConnectionTimeoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.connection_timeout_dialog_title))
            .setMessage(getString(R.string.connection_timeout_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.connection_timeout_dialog_button) { _, _ -> }
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
                setForecast(viewModel.getForecastResult)
                showDayForecast()
                viewModel.setSpinnerVisibilityMainFragment(false)
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

    private suspend fun setLocationGetForecast(location: Location): String {
        viewModel.setLocation(location)
        viewModel.currentLocation.let {
            return viewModel.getForecastByCoords(it.latitude, it.longitude)
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

    //@SuppressLint("MissingPermission")
    suspend fun tryGetCurrentLocForecast() {//TODO add permission exception handling
        lateinit var location: Location
        val locationManager: LocationManager = /*context /*was leaking*/*/
            activity?.applicationContext
                ?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var lastKnownLocationByGps: Location? = null
        var lastKnownLocationByNetwork: Location? = null
        try {
            lastKnownLocationByGps =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationByNetwork =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            Log.d(TAG, "Security exception: $e")
            showGeoPermissionRequiredDialog()
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
                GetLocationByGpsErrors.MISSING_PERMISSION -> {
                    Log.d("MainFragment", "error=$error")
                    showGeoPermissionRequiredDialog()
                    return
                }
            }
            location = newLocation
        }
        val exception = setLocationGetForecast(location)
        if (exception != Constants.emptyException) {
            Log.d(TAG, exception)
            showConnectionTimeoutDialog()
        }
        //viewModel.setSpinnerVisibilityMainFragment(false)
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

    //@SuppressLint("MissingPermission")
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

    private suspend fun onShowForecastButtonClicked() {
        viewModel.setSpinnerVisibilityMainFragment(true)
        if (!isNetworkAvailable(activity?.applicationContext)) {
            showNoInternetDialog()
            viewModel.setSpinnerVisibilityMainFragment(false)
        } else {
            if (viewModel.selectedCity == viewModel.emptyCity) {
                checkPermDetectLoc(viewModel.requestLocPermissionLauncher)
            } else /*MainViewModel.LocSetOptions.SELECT*/ {
                if (viewModel.selectedCity.name == null) {
                    Snackbar.make(
                        binding.showForecastButton,
                        getString(R.string.select_city_snackbar),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                } else {
                    viewModel.selectedCity.let {
                        if (it.latitude != null && it.longitude != null) {
                            val exception = viewModel.getForecastByCoords(it.latitude, it.longitude)
                            if (exception != Constants.emptyException) {
                                showConnectionTimeoutDialog()
                            } else {
                                setForecast(viewModel.getForecastResult)
                                showDayForecast()
                            }
                        }
                    }
                }
                viewModel.setSpinnerVisibilityMainFragment(false)
            }
            /* moving to both when branches, cause had to somehow wait for activitylauncher,
        mb should move to distinct function and pass it as lambda in higher order fun or just call it or smth
        setForecast(viewModel.getForecastResult.value)
        showDayForecast()
        viewModel.setSpinnerVisibilityMainFragment(false)*/
        }
    }

    private fun setForecast(forecastResult: ForecastResponse) {
        Log.d("MainFragment", "entered setForecast, forecastResult=$forecastResult")
        val weekForecast = handleForecastResponse(forecastResult)
        if (weekForecast[0].latitude != null) { //TODO mb replace this check with something more elegant
            Log.d(TAG, "weekForecast[0].latitude != null")
            viewModel.setWeekForecast(weekForecast)
        } else {
            Log.d(TAG, "weekForecast[0].latitude == null")
            viewModel.resetWeekForecast()
        }
    }

    private fun showDayForecast() {
        if (viewModel.weekForecast.isEmpty()) {
            binding.todayForecastTextView.text = ""
        } else {
            val todayForecast = viewModel.weekForecast[0]
            binding.todayForecastTextView.text = getString(
                R.string.day_forecast,
                todayForecast.temperature2mMin ?: "",
                todayForecast.temperature2mMax ?: "",
                todayForecast.weather ?: "",
                todayForecast.pressure ?: "",
                todayForecast.windspeed10mMax ?: "",
                todayForecast.winddirection10mDominant ?: "",
                todayForecast.relativeHumidity ?: ""
            )
        }
        binding.weekForecastButton.isEnabled =
            (viewModel.weekForecast.isNotEmpty())
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
}