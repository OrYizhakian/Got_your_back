import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.GotYourBack.R
import com.example.GotYourBack.databinding.BusinessDetailsBinding
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import kotlinx.coroutines.launch

class BusinessDetailsFragment : Fragment() {

    private var _binding: BusinessDetailsBinding? = null
    private val binding get() = _binding!!


    private lateinit var business: Business

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BusinessDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disable edit button until business is loaded
        binding.editButton.isEnabled = false

        lifecycleScope.launch {
            val businessId = arguments?.getLong("businessId") ?: -1L
            if (businessId == -1L) {
                Toast.makeText(requireContext(), "Business ID is missing.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@launch
            }

            val fetchedBusiness = AppDatabase.getDatabase(requireContext()).businessDao().getBusinessById(businessId)

            if (fetchedBusiness != null) {
                business = fetchedBusiness
                binding.editButton.isEnabled = true // Enable edit button
                displayBusinessDetails(business)
            } else {
                Toast.makeText(requireContext(), "Business not found.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun displayBusinessDetails(business: Business) {
        binding.businessNameText.text = business.name
        binding.businessCategoryText.text = business.category
        binding.businessAddressText.text = "${business.street} ${business.streetNumber}"

        // אם שעות הסגירה ריקות
        binding.businessHoursText.text =
            if (business.openingHours.isNotEmpty() && business.closingHours.isNotEmpty()) {
                "${business.openingHours} - ${business.closingHours}"
            } else {
                "Hours not available"
            }

        binding.businessDescriptionText.text = business.description ?: "No description provided"
    }


    private fun toggleEditMode() {
        binding.editButton.setOnClickListener {
            if (::business.isInitialized) {
                val bundle = Bundle().apply {
                    putLong("userId", business.userId)
                    putString("name", business.name)
                    putString("category", business.category)
                    putString("street", business.street ?: "")
                    putString("streetNumber", business.streetNumber ?: "")
                    putString("openingHours", business.openingHours ?: "")
                    putString("closingHours", business.closingHours ?: "")
                    putString("description", business.description ?: "")
                }
                findNavController().navigate(
                    R.id.action_businessDetailsFragment_to_businessUpdateFragment, bundle
                )
            } else {
                // Handle the case where the business is not yet initialized
                Toast.makeText(requireContext(), "Business data is not available yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
