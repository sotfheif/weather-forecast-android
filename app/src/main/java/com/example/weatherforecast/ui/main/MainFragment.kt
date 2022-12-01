package com.example.weatherforecast.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

class MainFragment : Fragment() {

    /*
    companion object {
        fun newInstance() = MainFragment()
    }
    */
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var weatherCodeMap: Map<Int, String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.requestLocPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        tryGetCurrentLocForecast()  //TODO maybe better move this call somewhere else
                        //viewModel.setSpinnerVisibilityMainFragment(false)
                    }
                } else {
                    //binding.statusImage.visibility = View.GONE
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
            if (viewModel.locationSettingOption.value ==
                MainViewModel.LocSetOptions.CURRENT
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setSpinnerVisibilityMainFragment(true)
                    checkPermDetectLoc(viewModel.requestLocPermissionLauncher)
                }
            } else if (viewModel.locationSettingOption.value ==
                MainViewModel.LocSetOptions.SELECT
            ) {
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
                                viewModel.setSpinnerVisibilityMainFragment(true)
                                viewModel.getForecastByCoords(it1, it2)
                            }
                        }
                    }
                }
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

        binding.selectCityButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
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


        viewModel.getForecastResult.observe(this.viewLifecycleOwner) {
            val weekForecast = handleForecastResponse(it)
            if (weekForecast[0].latitude != null) {
                viewModel.setWeekForecast(weekForecast)
            } else {
                viewModel.resetWeekForecast()
            }
        }
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

        viewModel.weekForecast.observe(this.viewLifecycleOwner) {
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
            .setMessage(getString(R.string.location_permission_rationale_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.location_permission_rationale_pos_button)) { _, _ ->
                activityResultLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            .setNegativeButton(R.string.location_permission_rationale_neg_button) { _, _ -> }
            .show()
    }

    private suspend fun checkPermDetectLoc(activityResultLauncher: ActivityResultLauncher<String>) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                tryGetCurrentLocForecast()
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

    fun setLocationGetForecast(location: Location?) {
        if (!viewModel.setLocation(location)) {
            showFailedToDetectGeoDialog()
        } else {
            viewModel.currentLocation.let {
                viewModel.getForecastByCoords(it.latitude, it.longitude)
            }
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
    fun tryGetCurrentLocForecast() {//TODO add permission exception handling
        val locationManager: LocationManager = context
            ?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocationByGps =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastKnownLocationByNetwork =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        //getting latest known location (from gps or network)
        val latestKnownLocResult =
            chooseLatestLocation(lastKnownLocationByGps, lastKnownLocationByNetwork)
        /* replaced by chooseLatestLocation call
        val lastKnownLocationByGpsTime = lastKnownLocationByGps?.time ?: 0
        val lastKnownLocationByNetworkTime = lastKnownLocationByNetwork?.time ?: 0
        var latestKnownLocTime: Long = 0
        var latestKnownLoc: Location? = null
        if (lastKnownLocationByGps != null) {
            latestKnownLoc = lastKnownLocationByGps
            latestKnownLocTime = lastKnownLocationByGpsTime
        }
        if (lastKnownLocationByNetwork != null &&
            lastKnownLocationByNetworkTime > lastKnownLocationByGpsTime
        ) {
            latestKnownLoc = lastKnownLocationByNetwork
            latestKnownLocTime = lastKnownLocationByNetworkTime
        }
        */
        var location = latestKnownLocResult.second
        if (latestKnownLocResult.first && location != null &&
            (location.time) >
            (Calendar.getInstance().timeInMillis - Constants.maxLocAge)
        ) {
            setLocationGetForecast(location)
            return
        }
        getLocationByGps(locationManager)
        //if no fresh enough location is present, get location by gps
        /*replaced by getLocationByGps call
        var myjob: Job? = null
        val gpsLocationListener: LocationListener =
            object : LocationListener {
                override fun onLocationChanged(locationp: Location) {
                    locationManager.removeUpdates(this)
                    myjob?.cancel()
                    setLocationGetForecast(locationp)
                }

                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                    locationManager.removeUpdates(this)
                    myjob?.cancel()
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showNoGeoDialog()
                }
            }
        if (locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0F,
                gpsLocationListener
            )
            myjob = viewLifecycleOwner.lifecycleScope.launch {
                repeat(20) {
                    delay(500)
                    yield()
                }
                location =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    ) ?: locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                if (location !=null) {
                    setLocationGetForecast(location)
                    //location?.let { setLocationGetForecast(it) }
                }
                else {showFailedToDetectGeoDialog()}
                //viewModel.setSpinnerVisibilityMainFragment(false)
            }
        } else  {
            viewModel.setSpinnerVisibilityMainFragment(false)
            showNoGeoDialog()
        }
        */
    }

    fun chooseLatestLocation(
        latestKnownLocation1: Location?,
        latestKnownLocation2: Location?
    ): Pair<Boolean, Location?> {
        if (latestKnownLocation1 == null && latestKnownLocation2 == null) {
            return Pair(false, null)
        }
        return if ((latestKnownLocation1?.time ?: 0) > (latestKnownLocation2?.time ?: 0)) {
            Pair(true, latestKnownLocation1)
        } else Pair(true, latestKnownLocation2)
    }

    @SuppressLint("MissingPermission")
    fun getLocationByGps(locationManager: LocationManager) {
        //TODO add permission exception handling (mb just in parent func)
        //
        var myjob: Job? = null
        val gpsLocationListener: LocationListener =
            object : LocationListener {
                override fun onLocationChanged(locationp: Location) {
                    locationManager.removeUpdates(this)
                    myjob?.cancel()
                    setLocationGetForecast(locationp)
                }

                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                    locationManager.removeUpdates(this)
                    myjob?.cancel()
                    viewModel.setSpinnerVisibilityMainFragment(false)
                    showNoGeoDialog()
                }
            }
        if (locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0F,
                gpsLocationListener
            )
            myjob = viewLifecycleOwner.lifecycleScope.launch {
                repeat(20) {
                    delay(500)
                    yield()
                }
                val location =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    ) ?: locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                if (location != null) {
                    setLocationGetForecast(location)
                    //location?.let { setLocationGetForecast(it) }
                } else {
                    showFailedToDetectGeoDialog()
                }
                //viewModel.setSpinnerVisibilityMainFragment(false)
            }
        } else {
            viewModel.setSpinnerVisibilityMainFragment(false)
            showNoGeoDialog()
        }
    }
}