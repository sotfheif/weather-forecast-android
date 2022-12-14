package com.github.sotfheif.weatherforecast.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.sotfheif.weatherforecast.databinding.FragmentCityBinding
import com.github.sotfheif.weatherforecast.network.City


class  MyCityListAdapter(private val onItemClicked: (City) -> Unit) :
    ListAdapter<City, MyCityListAdapter.MyCityViewHolder>(DiffCallback) {

    class MyCityViewHolder(private var binding: FragmentCityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(city: City) {//viewModel.prep
            binding.name.text = listOfNotNull(
                city.name, city.admin4, city.admin3,
                city.admin2, city.admin1, city.country,
            )
                .joinToString(", ")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCityViewHolder {
        val viewHolder = MyCityViewHolder(
            FragmentCityBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            onItemClicked(getItem(position))
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyCityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<City>() {
            override fun areItemsTheSame(oldItem: City, newItem: City): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: City, newItem: City): Boolean {
                return oldItem == newItem
            }
        }
    }

}