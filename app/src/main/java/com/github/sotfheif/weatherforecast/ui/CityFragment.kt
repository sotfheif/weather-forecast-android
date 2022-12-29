package com.github.sotfheif.weatherforecast.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sotfheif.weatherforecast.R
import com.github.sotfheif.weatherforecast.databinding.FragmentCityListBinding
import com.github.sotfheif.weatherforecast.ui.main.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class CityFragment :
    Fragment() {
    private var _binding: FragmentCityListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setNormalAppUiState()
        val myCityListAdapter = MyCityListAdapter {
            viewModel.setSelectedCity(it)
            viewModel.resetWeekForecast()
            val action = CityFragmentDirections.actionCityFragmentToMainFragment()
            view.findNavController().navigate(action)//TODO later mb prevent doubleclick?
        }
            binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
            binding.recyclerView.adapter = myCityListAdapter

        viewModel.citySearchResult.let {
            if (it.isNotEmpty()) {
                myCityListAdapter.submitList(it)
            }
            else {
                showUnexpectedMistake()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showUnexpectedMistake(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.unexpected_mistake_text))
            .setMessage(getString(R.string.unexpected_mistake_text))
            .setCancelable(true)
            .setNegativeButton(R.string.no_geo_dialog_button){_, _ ->}
            .show()
    }
}