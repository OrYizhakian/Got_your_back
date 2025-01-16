import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gis_test.databinding.BusinessItemBinding
import com.example.gis_test.data.Business

class BusinessAdapter(private val onLongPress: (Business) -> Unit) :
    RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    private val businesses = mutableListOf<Business>()

    fun submitList(newBusinesses: List<Business>) {
        businesses.clear()
        businesses.addAll(newBusinesses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val binding =
            BusinessItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BusinessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = businesses[position]
        holder.bind(business)

        // Handle long-press to trigger the callback
        holder.itemView.setOnLongClickListener {
            onLongPress(business)
            true
        }
    }

    override fun getItemCount(): Int = businesses.size


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
}
