package com.github.sotfheif.weatherforecast.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.sotfheif.weatherforecast.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}