package com.github.sotfheif.weatherforecast.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.github.sotfheif.weatherforecast.R
import com.github.sotfheif.weatherforecast.data.DayForecast
import com.github.sotfheif.weatherforecast.databinding.FragmentMainBinding
import com.github.sotfheif.weatherforecast.prepForUi
import com.github.sotfheif.weatherforecast.pressureToLocUnit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.util.*


//private const val TAG = "MainFragment" //DEBUG FEATURE
class MainFragment : Fragment() {
    //TODO BEFORE RELEASE: remove/replace unexpectedmistake dialog and all code commented as debug feature, replace connection timeout with connection error.
    //TODO BUG open weekforecastfragment, change locale, return to the app(weekforecastfragment). weathercode remains the same)
    //TODO make "enter" press selectcitybutton
    //TODO make possible typing location name in other language.
    //TODO mb show pressure in other units like mB or mm Hg
    //TODO check PackageFunctions DayForecast.prepForUi
    //TODO OK FOR NOW after app launch first button click (showForecast or selectCity with textinput no blank) is laggy, probably due to fat viewmodel lazy initialization
    //TODO BUG mb resolved. when shouldshowrationale is true if showForecastButton is clicked consequently fast enough, several rationaleDialogs appear
    //TODO BUG if after install and launch showforecast is clicked quickly successively and then perm is denied, there wil be 2 no_perm dialogs
    //TODO check that where necessary additional function calls are prevented (like after fast successive buttonclicks) or that some functions stop after conflicting functions are called
    //TODO BUG when clicked showforecast fast successively, spinner may continue being visible after no_internet dialog, and then (long after the dialog closed) dissappear without  anything shown
    //TODO check setSpinnerVisibility() placement (in code).
    //TODO mb make showForecasButtonWork, selectCityButtonWork volatile or use existing loading spinner livedata instead
    //TODO later prompt to turn on android location (gps) inside app
    //TODO later use fusedlocationprovider from google on devices where available
    //TODO later add possibility to save location (maybe just latest detected current location will do), so that you can later use that saved location
    //TODO later review architecture
    //TODO later replace "current location" in selected city text view with detected location name
    //TODO later download web service's location (cities) db (update regularly in background), and make search with spinner so that possible options are shown and updated after every char entered/deleted
    //TODO later in search field (before any chars entered) show previous location search queries(or selected results(locations)?
    //TODO later send notifications about bad weather? maybe even set human speech as notif sound (like "rain today")
    /*
    companion object {
        fun newInstance() = MainFragment()
    }
    */
    private val viewModel: MainViewModel by activityViewModels() //or another way?
    private lateinit var binding: FragmentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate timber")
        viewModel.requestLocPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
                -> {
                    viewModel.setShowForecastButtonWork(false)
                    if (!isNetworkAvailable(activity?.applicationContext)) {
                        viewModel.setAppUiState(MainViewModel.AppUiStates.NO_INTERNET)//showNoInternetDialog()
                    } else {
                        viewModel.tryGetSetCurrentLocForecast()  //TODO maybe better move this call somewhere else
                    }
                    //viewModel.setSpinnerVisibilityMainFragment(false)
                }
                else -> {
                    // No location access granted.
                    viewModel.setShowForecastButtonWork(false)
                    viewModel.setAppUiState(MainViewModel.AppUiStates.GEO_PERM_REQUIRED)
                }
            }
        }

        /*
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
     */
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView")
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        binding.showForecastButton.setOnClickListener {
            viewModel.onShowForecastButtonClicked() // was fragment's onShowForecastButtonClicked()
        }
        binding.currentLocationButton.setOnClickListener {
            Timber.d("CurrentLocationButton onclicklistener")
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
            Timber.d(
                "set selectedCityTextView.text = ${getString(R.string.selected_city_text_current_location)}"
            )
        }

        binding.selectCityButton.setOnClickListener {
            onSelectCityButtonClicked(getString(R.string.language))
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
                MainViewModel.AppUiStates.NO_INTERNET,
                MainViewModel.AppUiStates.CONNECTION_TIMEOUT -> showNoInternetDialog()
                //MainViewModel.AppUiStates.CONNECTION_TIMEOUT -> showConnectionTimeoutDialog() united with connectionerrordialog for simplicity
                //MainViewModel.AppUiStates.UNEXPECTED_MISTAKE -> showUnexpectedMistake() //DEBUG FEATURE
                MainViewModel.AppUiStates.CITY_NOT_FOUND -> showCityNotFoundDialog()
                MainViewModel.AppUiStates.GO_TO_CITY_FRAGMENT ->// try {
                    this@MainFragment
                        .findNavController().navigate(
                            MainFragmentDirections.actionMainFragmentToCityFragment()
                        )
                //}catch (_: Throwable){ }
                //MainViewModel.AppUiStates.LAT_OR_LONG_NULL -> showLatOrLongNullDialog()// DEBUG FEATURE
                MainViewModel.AppUiStates.EMPTY_CITY_TEXT_FIELD -> Snackbar.make(
                    binding.showForecastButton,
                    getString(R.string.enter_city_snackbar),
                    Snackbar.LENGTH_SHORT
                ).show()
                MainViewModel.AppUiStates.CHECK_LOC_PERM -> checkPermDetectLoc(viewModel.requestLocPermissionLauncher)
                MainViewModel.AppUiStates.API_ERROR -> showApiErrorDialog()
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

        viewModel.weatherCodeMap = mapOf(
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
                binding.selectCityButton.isEnabled = false
                binding.currentLocationButton.isEnabled = false
            } else {
                binding.statusImageForecast.visibility = View.GONE
                binding.selectCityButton.isEnabled = true
                binding.currentLocationButton.isEnabled = true
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
            Timber.d("onSaveInstanceState")
        }

        override fun onStart() {
            super.onStart()
            Timber.d("onStart")
        }

        override fun onPause() {
            super.onPause()
            Timber.d("onPause")
        }

        override fun onResume() {
            super.onResume()
            Timber.d("onResume")
        }

        override fun onStop() {
            super.onStop()
            Timber.d("onStop")
        }

        override fun onDestroy() {
            super.onDestroy()
            Timber.d("onDestroy")
        }

        override fun onDestroyView() {
            super.onDestroyView()
            Timber.d("onDestroyView")
        }
    */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Timber.d("onViewStateRestored")
        viewModel.selectedCity.let {
            binding.selectedCityTextView.text = if (it == viewModel.emptyCity)
                getString(
                    R.string.selected_location_text_prefix,
                    getString(R.string.selected_city_text_current_location)
                )
            else getString(R.string.selected_location_text_prefix, it.prepForUi())
        }
    }


    private fun showNoGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_geo_dialog_title))
            .setMessage(getString(R.string.no_geo_dialog_text))
            .setCancelable(true)
            .setPositiveButton(R.string.no_geo_dialog_positive_button) { _, _ ->
                if (viewModel.shouldOpenLocSettings())
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(R.string.no_geo_dialog_negative_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showFailedToDetectGeoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.failed_to_detect_geo_dialog_title))
            .setMessage(
                if (Build.VERSION.SDK_INT < 30)
                    getString(R.string.failed_to_detect_geo_dialog_text_lt_api30)
                else getString(R.string.failed_to_detect_geo_dialog_text_ge_api30)
            )
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
            .setNegativeButton(R.string.city_not_found_dialog_negative_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    /*
        private fun showUnexpectedMistake() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.unexpected_mistake_text))
                .setMessage(getString(R.string.unexpected_mistake_text))
                .setCancelable(true)
                .setNegativeButton(R.string.no_geo_dialog_button) { _, _ -> }
                .setOnDismissListener { viewModel.setNormalAppUiState() }
                .show()
        }
    */
    private fun showNoInternetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_internet_dialog_title))
            .setMessage(getString(R.string.no_internet_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_internet_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    private fun showApiErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.api_error_dialog_title))
            .setMessage(getString(R.string.api_error_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.api_error_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }
/* // using only connectionErrorDialog for simplicity
    private fun showConnectionTimeoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.connection_timeout_dialog_title))
            .setMessage(getString(R.string.connection_timeout_dialog_text))
            .setCancelable(true)
            .setNegativeButton(R.string.connection_timeout_dialog_button) { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }
*/
/*
    private fun showLatOrLongNullDialog() { //DEBUG FEATURE
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Latitude or longitude null")
            .setMessage("After getting location latitude or longitude appear to be null")
            .setCancelable(true)
            .setNegativeButton("Close") { _, _ -> }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }
*/

    private fun showGeoPermissionRationaleDialog(activityResultLauncher: ActivityResultLauncher<Array<String>>) {
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
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                )
            }
            .setNegativeButton(R.string.location_permission_rationale_neg_button) { _, _ ->
                viewModel.setForecastSpinnerVisibilityMainFragment(false)
            }
            .setOnDismissListener { viewModel.setNormalAppUiState() }
            .show()
    }

    fun checkPermDetectLoc(activityResultLauncher: ActivityResultLauncher<Array<String>>) {

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.d("coarselocpermgranted when branch")
                viewModel.tryGetSetCurrentLocForecast()
                //prepDayForecastUiText()
                //viewModel.setSpinnerVisibilityMainFragment(false)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                //viewModel.setSpinnerVisibilityMainFragment(false)
                Timber.d("rationalecoarseloc when branch")
                viewModel.setAppUiState(MainViewModel.AppUiStates.GEO_PERM_RATIONALE) //showGeoPermissionRationaleDialog(activityResultLauncher)
            }
            else -> {
                Timber.d("else when branch")
                //viewModel.setShowForecastButtonWork(true)
                activityResultLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
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
    suspend fun tryGetCurrentLocForecast() {
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
            Timber.d("Security exception: $e")
            showGeoPermissionRequiredDialog()
            return
        } catch (e: Exception) {
            Timber.d("Unexpected exception: ${e})")
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
            Timber.d("line after newLocation, error assignment")
            when (error) {
                GetLocationByGpsErrors.GPS_IS_OFF -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showNoGeoDialog(); return
                }
                GetLocationByGpsErrors.LOC_DETECTION_FAILED -> {
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    Timber.d("error=$error")
                    showFailedToDetectGeoDialog(); return
                }
                GetLocationByGpsErrors.NO_ERROR -> {
                    if (newLocation == null) {
                        Timber.d("error=$error, newLocation == null")
                        viewModel.setSpinnerVisibilityMainFragment(false)
                        showUnexpectedMistake(); return
                    }
                }
                GetLocationByGpsErrors.MISSING_PERMISSION -> {
                    Timber.d("error=$error")
                    showGeoPermissionRequiredDialog()
                    return
                }
            }
            location = newLocation
        }
        val exception = setLocationGetForecast(location)
        if (exception != Constants.emptyException) {
            Timber.d(exception)
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
/* replaced by viewmodel's fun with the same name
    fun onShowForecastButtonClicked() {
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
                    viewModel.setAppUiState(MainViewModel.AppUiStates.EMPTY_CITY_TEXT_FIELD)
                    viewModel.setNormalAppUiState()
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
*/
/*
    private fun setForecast(forecastResult: ForecastResponse) {
        Timber.d("entered setForecast, forecastResult=$forecastResult")
        val weekForecast = handleForecastResponse(forecastResult)
        if (weekForecast[0].latitude != null) {
            Timber.d("weekForecast[0].latitude != null")
            viewModel.setWeekForecast(weekForecast)
        } else {
            Timber.d("weekForecast[0].latitude == null")
            viewModel.resetWeekForecast()
        }
    }
    */

    fun prepDayForecastUiText(dayForecast: DayForecast): String {
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
                viewModel.weatherCodeMap[dayForecast.weather?.toInt()] ?: "",
                dayForecast.pressure?.pressureToLocUnit(requireContext()) ?: "",
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

    private fun onSelectCityButtonClicked(citySearchQueryLang: String) {
        //if (viewModel.selectCityButtonWork) return // moved this into beginning of viewModel.checkknetworkfindcity. or move here setting work to true from viewModel.findCity
        closeVirtualKeyboard()
        if (binding.textFieldInput.text.isNullOrBlank()) {
            viewModel.setAppUiState(MainViewModel.AppUiStates.EMPTY_CITY_TEXT_FIELD)
            viewModel.setNormalAppUiState()
        } else viewModel.checkNetworkFindCity(
            binding.textFieldInput.text.toString(),
            citySearchQueryLang
        )
        /*if (isNetworkAvailable(activity?.applicationContext)) {
                viewModel.findCity(binding.textFieldInput.text.toString())
            } else viewModel.setAppUiState(MainViewModel.AppUiStates.NO_INTERNET)*/
    }

}