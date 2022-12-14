package com.github.sotfheif.weatherforecast.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sotfheif.weatherforecast.databinding.FragmentItemListBinding
import com.github.sotfheif.weatherforecast.ui.main.MainViewModel

class WeekForecastFragment : Fragment() {
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setNormalAppUiState()
        val myWeekForecastListAdapter = MyWeekForecastListAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.recyclerView.adapter = myWeekForecastListAdapter
        myWeekForecastListAdapter.submitList(viewModel.weekForecast.value?.map {
            viewModel.weatherCodeToWords(it)
        })
    }
}