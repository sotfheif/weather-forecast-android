package com.example.weatherforecast.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherforecast.R
import com.example.weatherforecast.data.DayForecast
import com.example.weatherforecast.databinding.FragmentItemBinding
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
                holder.itemView.context.getString(
                    R.string.day_forecast,
                    dayForecast.temperature2mMin ?: "",
                    dayForecast.temperature2mMax ?: "",
                    dayForecast.weather ?: "",
                    dayForecast.pressure ?: "",
                    dayForecast.windspeed10mMax ?: "",
                    dayForecast.winddirection10mDominant ?: "",
                    dayForecast.relativeHumidity ?: ""
                )
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