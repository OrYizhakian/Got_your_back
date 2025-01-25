package com.example.gis_test.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gis_test.data.Business
import com.example.gis_test.databinding.BusinessItemBinding

class BusinessAdapter(
    private val onShortClick: (Business) -> Unit,
    private val onLongPress: (Business) -> Unit
) : ListAdapter<Business, BusinessAdapter.BusinessViewHolder>(BusinessDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val binding = BusinessItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BusinessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = getItem(position)
        holder.bind(business)

        holder.itemView.setOnClickListener { onShortClick(business) }
        holder.itemView.setOnLongClickListener {
            onLongPress(business)
            true
        }
    }

    class BusinessViewHolder(private val binding: BusinessItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(business: Business) {
            binding.apply {
                businessName.text = business.name
                businessCategory.text = business.category
                businessAddress.text = "${business.street} ${business.streetNumber}"
                businessHours.text = "${business.openingHours} - ${business.closingHours}"
                businessDescription.text = business.description ?: ""
            }
        }
    }

    class BusinessDiffCallback : DiffUtil.ItemCallback<Business>() {
        override fun areItemsTheSame(oldItem: Business, newItem: Business): Boolean =
            oldItem.businessId == newItem.businessId

        override fun areContentsTheSame(oldItem: Business, newItem: Business): Boolean =
            oldItem == newItem
    }
}