package com.example.weatherforecast.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherforecast.R
import com.example.weatherforecast.databinding.FragmentCityListBinding
import com.example.weatherforecast.network.City
import com.example.weatherforecast.ui.main.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar


class CityFragment :
    Fragment() { //TODO EXPAND CITY LIST ELEMENTS IN UI TO PARENT'S WIDTH, SO THAT CLICKING ON TRAILING WHITESPACE IN ELEMENT OF THE LIST COUNTS AS CLICKING THE ELEMENT
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
        val myCityListAdapter = MyCityListAdapter {
            viewModel.setSelectedCity(it)
            val action = CityFragmentDirections.actionCityFragmentToMainFragment()
            view.findNavController().navigate(action)
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

        viewModel.statusImageCityFragment.observe(this.viewLifecycleOwner) {
            if (it) {
                binding.statusImage.visibility = View.VISIBLE
            } else {
                binding.statusImage.visibility = View.GONE
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