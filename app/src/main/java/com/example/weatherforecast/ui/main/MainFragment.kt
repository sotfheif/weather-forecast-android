package com.example.weatherforecast.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.weatherforecast.R
import com.example.weatherforecast.data.DayForecast
import com.example.weatherforecast.databinding.FragmentMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.*

private const val TAG = "MainFragment"
class MainFragment : Fragment() {
    //TODO BUG when shouldshowrationale is true if showForecastButton is clicked consequently fast enough, several rationaleDialogs appear
    //TODO check that where necessary additional function calls are prevented (like after fast successive buttonclicks) or that some functions stop after conflicting functions are called
    //TODO test connection unavailable
    //TODO make "enter" press selectcitybutton
    //TODO mb BUG when internet is turned off after pressing showfirecast "connection timeout" dialog appears
    //TODO BUG sometimes forecast won't appear in ui. mb is necessary for internet speed to be low. as one of solutions can  after clicking show forecst once more it appears
    //TODO check setSpinnerVisibility placement.
    //TODO later review architecture
    //TODO later download web service's location (cities) db (update regularly in background), and make search with spinner so that possible options are shown and updated after every char entered/deleted
    //TODO later in search field (before any chars entered) show previous location search queries(or selected results(locations)?
    /*
    companion object {
        fun newInstance() = MainFragment()
    }
    */
    private val viewModel: MainViewModel by activityViewModels() //or another way?
    private lateinit var binding: FragmentMainBinding
    private lateinit var weatherCodeMap: Map<Int, String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        viewModel.requestLocPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.setShowForecastButtonWork(false)
                        if (!isNetworkAvailable(activity?.applicationContext)) {
                            viewModel.setAppUiState(MainViewModel.AppUiStates.NO_INTERNET)//showNoInternetDialog()
                        } else {
                            viewModel.tryGetSetCurrentLocForecast()  //TODO maybe better move this call somewhere else
                            //prepDayForecastUiText()
                        }
                        //viewModel.setSpinnerVisibilityMainFragment(false)
                } else {
                    viewModel.setShowForecastButtonWork(false)
                    viewModel.setAppUiState(MainViewModel.AppUiStates.GEO_PERM_REQUIRED)//showGeoPermissionRequiredDialog()
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
            onShowForecastButtonClicked()
        }
        binding.currentLocationButton.setOnClickListener {
            Log.d(TAG, "CurrentLocationButton onclicklistener")
            closeVirtualKeyboard()
            if (viewModel.selectedCity == viewModel.emptyCity) {
                return@setOnClickListener
            }
            viewModel.resetWeekForecast()
            binding.weekForecastButton.isEnabled =
                (viewModel.weekForecast.value?.isNotEmpty() == true)
            binding.todayForecastTextView.text = ""
            viewModel.resetSelectedCity()
            binding.selectedCityTextView.text =
                getString(
                    R.string.selected_location_text_prefix,
                    getString(R.string.selected_city_text_current_location)
                )
            Log.d(
                TAG,
                "set selectedCityTextView.text = ${getString(R.string.selected_city_text_current_location)}"
            )
        }

        binding.selectCityButton.setOnClickListener {//TODO  mb extract THIS BLOCK OF CODE INTO A FUNCTION
            onSelectCityButtonClicked()
        }

        binding.weekForecastButton.setOnClickListener {
            try {
                this.findNavController()
                    .navigate(MainFragmentDirections.actionMainFragmentToItemFragment())
            } catch (_: Throwable) {
            }
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

        viewModel.appUiState.observe(this.viewLifecycleOwner) {
            when (it) {
                MainViewModel.AppUiStates.NORMAL -> {}
                MainViewModel.AppUiStates.GEO_PERM_REQUIRED -> showGeoPermissionRequiredDialog()
                MainViewModel.AppUiStates.GEO_DETECT_FAILED -> showFailedToDetectGeoDialog()
                MainViewModel.AppUiStates.GEO_PERM_RATIONALE -> showGeoPermissionRationaleDialog(
                    viewModel.requestLocPermissionLauncher
                )
                MainViewModel.AppUiStates.NO_GEO -> showNoGeoDialog()
                MainViewModel.AppUiStates.NO_INTERNET -> showNoInternetDialog()
                MainViewModel.AppUiStates.CONNECTION_TIMEOUT -> showConnectionTimeoutDialog()
                MainViewModel.AppUiStates.UNEXPECTED_MISTAKE -> showUnexpectedMistake()
                MainViewModel.AppUiStates.WAITING_GEO -> {}
                MainViewModel.AppUiStates.WAITING_CITY_SEARCH -> {}
                MainViewModel.AppUiStates.WAITING_FORECAST_RESPONSE -> {}
                MainViewModel.AppUiStates.CITY_NOT_FOUND -> showCityNotFoundDialog()
                MainViewModel.AppUiStates.GO_TO_CITY_FRAGMENT ->// try {
                    this@MainFragment
                        .findNavController().navigate(
                            MainFragmentDirections.actionMainFragmentToCityFragment()
                        )
                //}catch (_: Throwable){ }
                MainViewModel.AppUiStates.LAT_OR_LONG_NULL -> showLatOrLongNullDialog()
                else -> {}//"w: enum arg can be null in java
            }
        }

        viewModel.weekForecast.observe(this.viewLifecycleOwner) {
            if (viewModel.weekForecast.value.isNullOrEmpty()) {
                binding.weekForecastButton.isEnabled = false
            } else {
                binding.todayForecastTextView.text = prepDayForecastUiText(it[0])
                binding.weekForecastButton.isEnabled = true
            }
        }

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
        viewModel.forecastStatusImageMainFragment.observe(this.viewLifecycleOwner)
        {
            if (it) {
                binding.statusImageForecast.visibility = View.VISIBLE
            } else {
                binding.statusImageForecast.visibility = View.GONE
            }
        }
        viewModel.cityStatusImageMainFragment.observe(this.viewLifecycleOwner) {
            if (it) {
                binding.selectCityButton.text = ""
                binding.statusImageCity.visibility = View.VISIBLE
            } else {
                binding.statusImageCity.visibility = View.GONE
                binding.selectCityButton.text = getString(R.string.select_city_button)
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
        //prepDayForecastUiText()
        viewModel.selectedCity.let {
            binding.selectedCityTextView.text = if (it == viewModel.emptyCity)
                getString(
                    R.string.selected_location_text_prefix,
                    getString(R.string.selected_city_text_current_location)
                )
            else getString(R.string.selected_location_text_prefix, viewModel.prepCityForUi(it))
        }
    }


    private fun showNoGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_dialog_title))
            .setMessage(getString(R.string.no_geo_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showFailedToDetectGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.failed_to_detect_geo_dialog_title))
            .setMessage(getString(R.string.failed_to_detect_geo_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.failed_to_detect_geo_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showGeoPermissionRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_permission_dialog_title))
            .setMessage(getString(R.string.no_geo_permission_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_permission_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showCityNotFoundDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.city_not_found_dialog_title))
            .setMessage(getString(R.string.city_not_found_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showUnexpectedMistake() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.unexpected_mistake_text))
            .setMessage(getString(R.string.unexpected_mistake_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showNoInternetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_internet_dialog_title))
            .setMessage(getString(R.string.no_internet_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_internet_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showConnectionTimeoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.connection_timeout_dialog_title))
            .setMessage(getString(R.string.connection_timeout_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.connection_timeout_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showLatOrLongNullDialog() { //TODO remove/replace in release build. DEBUG FEATURE
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Latitude or longitude null")
            .setMessage("After getting location latitude or longitude appear to be null")
            .setCancelable(true)
            .setNegativeButton("Close") { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
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
                viewModel.setForecastSpinnerVisibilityMainFragment(false)
            }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun checkPermDetectLoc(activityResultLauncher: ActivityResultLauncher<String>) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.tryGetSetCurrentLocForecast()
                //prepDayForecastUiText()
                //viewModel.setSpinnerVisibilityMainFragment(false)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                //viewModel.setSpinnerVisibilityMainFragment(false)
                viewModel.setAppUiState(MainViewModel.AppUiStates.GEO_PERM_RATIONALE) //showGeoPermissionRationaleDialog(activityResultLauncher)
            }
            else -> {
                viewModel.setShowForecastButtonWork(true)
                activityResultLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
    }
/*
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
*/

    /*
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
            Log.d(TAG, "string after newLocation, error assignment")
            when (error) {//TODO mb replace returns/spinnervisibilitysets, or leave one
                GetLocationByGpsErrors.GPS_IS_OFF -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showNoGeoDialog(); return
                }
                GetLocationByGpsErrors.LOC_DETECTION_FAILED -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    Log.d(TAG, "error=$error")
                    showFailedToDetectGeoDialog(); return
                }
                GetLocationByGpsErrors.NO_ERROR -> {
                    if (newLocation == null) {
                        Log.d(TAG, "error=$error, newLocation == null")
                        viewModel.setSpinnerVisibilityMainFragment(false)
                        showUnexpectedMistake(); return //TODO change showUnexpectedMistake showFailedToDetectGeo in release
                    }
                }
                GetLocationByGpsErrors.MISSING_PERMISSION -> {
                    Log.d(TAG, "error=$error")
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
    */
/*
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

*/

    private fun onShowForecastButtonClicked() {
        if (viewModel.showForecastButtonWork) return
        //viewModel.setSpinnerVisibilityMainFragment(true)
        if (!isNetworkAvailable(activity?.applicationContext)) {
            viewModel.setAppUiState(MainViewModel.AppUiStates.NO_INTERNET)
            //viewModel.setSpinnerVisibilityMainFragment(false)
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
                    viewModel.tryGetSelCityForecast()
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
/*
    private fun setForecast(forecastResult: ForecastResponse) {
        Log.d(TAG, "entered setForecast, forecastResult=$forecastResult")
        val weekForecast = handleForecastResponse(forecastResult)
        if (weekForecast[0].latitude != null) { //TODO mb replace this check with something more elegant
            Log.d(TAG, "weekForecast[0].latitude != null")
            viewModel.setWeekForecast(weekForecast)
        } else {
            Log.d(TAG, "weekForecast[0].latitude == null")
            viewModel.resetWeekForecast()
        }
    }
    */

    private fun prepDayForecastUiText(dayForecast: DayForecast): String {
        return if (viewModel.weekForecast.value.isNullOrEmpty()) {
            ""
        } else {
            getString(
                R.string.day_forecast,
                dayForecast.temperature2mMin?.plus(
                    getString(R.string.temperature_unit)
                ) ?: "",
                dayForecast.temperature2mMax?.plus(
                    getString(R.string.temperature_unit)
                ) ?: "",
                weatherCodeMap[dayForecast.weather?.toInt()] ?: "",
                dayForecast.pressure?.plus(
                    getString(R.string.pressure_unit)
                ) ?: "",
                dayForecast.windspeed10mMax?.plus(
                    getString(R.string.wind_speed_unit)
                ) ?: "",
                dayForecast.winddirection10mDominant?.plus(
                    getString(R.string.wind_direction_unit)
                ) ?: "",
                dayForecast.relativeHumidity?.plus(
                    getString(R.string.relative_humidity_unit)
                ) ?: ""
            )
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

    fun closeVirtualKeyboard() {
        ViewCompat.getWindowInsetsController(requireView())
            ?.hide(WindowInsetsCompat.Type.ime())
    }

    fun onSelectCityButtonClicked() {
        if (viewModel.selectCityButtonWork) return //TODO later mb move this into beginning of viewModel.findCity. or move here setting work to true from viewModel.findCity
        closeVirtualKeyboard()
        if (binding.textFieldInput.text.toString().isBlank()) {
            Snackbar.make(
                binding.showForecastButton,
                getString(R.string.enter_city_snackbar),
                Snackbar.LENGTH_SHORT
            )
                .show()
        } else if (isNetworkAvailable(activity?.applicationContext)) {
            viewModel.findCity(binding.textFieldInput.text.toString())
        } else viewModel.setAppUiState(MainViewModel.AppUiStates.NO_INTERNET)
    }

}