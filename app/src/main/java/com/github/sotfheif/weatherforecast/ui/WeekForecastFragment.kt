package com.github.sotfheif.weatherforecast.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sotfheif.weatherforecast.R
import com.github.sotfheif.weatherforecast.data.DayForecast
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
        val weatherCodeMap = mapOf(/*mb do something with this*/
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

        myWeekForecastListAdapter.submitList(viewModel.weekForecast.value?.map {
            weatherCodeToWords(it, weatherCodeMap)
        })
    }

    fun weatherCodeToWords(
        dayForecast: DayForecast,
        weatherCodeMap: Map<Int, String>
    ): DayForecast {
        return dayForecast.run {
            DayForecast(
                id, latitude, longitude, pressure, relativeHumidity,
                weatherCodeMap[weather?.toInt()], temperature2mMin, temperature2mMax,
                windspeed10mMax, winddirection10mDominant
            )
        }
    }
}