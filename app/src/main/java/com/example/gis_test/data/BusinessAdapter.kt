import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gis_test.databinding.BusinessItemBinding
import com.example.gis_test.data.Business

class BusinessAdapter : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    private val businesses = mutableListOf<Business>()

    /**
     * Updates the list of businesses displayed in the RecyclerView.
     */
    fun submitList(newBusinesses: List<Business>) {
        businesses.clear()
        businesses.addAll(newBusinesses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val binding = BusinessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BusinessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        holder.bind(businesses[position])
    }

    override fun getItemCount(): Int = businesses.size

    /**
     * ViewHolder class for displaying individual business items.
     */
    class BusinessViewHolder(private val binding: BusinessItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a [Business] object to the ViewHolder's layout.
         */
        fun bind(business: Business) {
            binding.businessName.text = business.name
            binding.businessCategory.text = business.category
            binding.businessAddress.text = "${business.street} ${business.streetNumber}"
            binding.businessHours.text = "${business.openingHours} - ${business.closingHours}"
            binding.businessDescription.text = business.description
        }
    }
}
