package com.github.sotfheif.weatherforecast.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.sotfheif.weatherforecast.R
import com.github.sotfheif.weatherforecast.data.DayForecast
import com.github.sotfheif.weatherforecast.databinding.FragmentItemBinding
import com.github.sotfheif.weatherforecast.pressureToLocUnit
import java.text.SimpleDateFormat
import java.util.*

class MyWeekForecastListAdapter :
    ListAdapter<DayForecast, MyWeekForecastListAdapter.MyItemViewHolder>(DiffCallback) {

    class MyItemViewHolder(private var binding: FragmentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dayForecast: DayForecast, position: Int, holder: MyItemViewHolder) {
            val dayInMillis = 1000 * 60 * 60 * 24
            binding.date.text = SimpleDateFormat("yyyy-MM-dd").format(
                Calendar.getInstance().timeInMillis + dayInMillis * position
            )
            binding.forecast.text =
                holder.itemView.context.run {
                    getString(
                        R.string.day_forecast,
                        dayForecast.temperature2mMin?.plus(
                            getString(R.string.temperature_unit)
                        ) ?: "",
                        dayForecast.temperature2mMax?.plus(
                            getString(R.string.temperature_unit)
                        ) ?: "",
                        dayForecast.weather ?: "",
                        dayForecast.pressure?.pressureToLocUnit(
                            this
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyItemViewHolder {
        return MyItemViewHolder(
            FragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyItemViewHolder, position: Int) {
        holder.bind(getItem(position), position, holder)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DayForecast>() {
            override fun areItemsTheSame(oldItem: DayForecast, newItem: DayForecast): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DayForecast, newItem: DayForecast): Boolean {
                return oldItem == newItem
            }
        }
    }

}