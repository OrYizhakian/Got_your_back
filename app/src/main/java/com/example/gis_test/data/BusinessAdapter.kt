import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gis_test.databinding.BusinessItemBinding
import com.example.gis_test.data.Business

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class BusinessAdapter(
    private val onShortClick: (Business) -> Unit,
    private val onLongPress: (Business) -> Unit
) : ListAdapter<Business, BusinessAdapter.BusinessViewHolder>(BusinessDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val binding =
            BusinessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BusinessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = getItem(position)
        holder.bind(business)

        // Handle short press to trigger the callback
        holder.itemView.setOnClickListener {
            onShortClick(business)
        }

        // Handle long press to trigger the callback
        holder.itemView.setOnLongClickListener {
            onLongPress(business)
            true
        }
    }

    class BusinessViewHolder(private val binding: BusinessItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(business: Business) {
            binding.businessName.text = business.name
            binding.businessCategory.text = business.category
            binding.businessAddress.text = "${business.street} ${business.streetNumber}"
            binding.businessHours.text = "${business.openingHours} - ${business.closingHours}"
            binding.businessDescription.text = business.description
        }
    }

    class BusinessDiffCallback : DiffUtil.ItemCallback<Business>() {
        override fun areItemsTheSame(oldItem: Business, newItem: Business): Boolean {
            return oldItem.businessId == newItem.businessId
        }

        override fun areContentsTheSame(oldItem: Business, newItem: Business): Boolean {
            return oldItem == newItem
        }
    }
}
