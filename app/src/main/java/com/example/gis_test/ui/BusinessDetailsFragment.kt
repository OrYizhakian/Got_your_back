import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.gis_test.databinding.BusinessDetailsBinding
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

        val businessId = arguments?.getLong("businessId") ?: -1L
        if (businessId == -1L) {
            // Handle missing business ID
            Toast.makeText(requireContext(), "Error: Business ID is missing.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Fetch business details from the database
        lifecycleScope.launch {
            business = AppDatabase.getDatabase(requireContext()).businessDao()
                .getBusinessById(businessId)!!
            if (business != null) {
                displayBusinessDetails(business)
            } else {
                Toast.makeText(requireContext(), "Error: Business not found.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Edit button click listener
        binding.editButton.setOnClickListener {
            toggleEditMode()
        }

        // Save button click listener
        binding.saveButton.setOnClickListener {
            val updatedBusiness =
                Business(userId = business.userId,  // השתמש ב-userId של העסק שנמצא ב-business
                    businessId = businessId,  // השתמש ב-businessId שקיבלת ב-arguments
                    name = binding.businessName.text.toString(),
                    category = binding.businessCategory.text.toString(),
                    street = binding.businessAddress.text.toString(),
                    streetNumber = "",  // תוודא שיש לך EditText בשם זה
                    openingHours = binding.businessHours.text.toString(),
                    closingHours = "",  // תוודא שיש לך EditText נוסף לשעות סגירה
                    description = binding.businessDescription.text.toString()
                        .takeIf { it.isNotEmpty() })

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).businessDao()
                    .updateBusiness(updatedBusiness)
                toggleEditMode()
                displayBusinessDetails(updatedBusiness)
            }
        }

        // Cancel button click listener
        binding.cancelButton.setOnClickListener {
            toggleEditMode()
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
            val bundle = Bundle().apply {
                putLong("userId", business.userId)
                putString("name", business.name)
                putString("category", business.category)
                putString("street", business.street ?: "") // טיפול ב-null
                putString("streetNumber", business.streetNumber ?: "") // טיפול ב-null
                putString("openingHours", business.openingHours ?: "") // טיפול ב-null
                putString("closingHours", business.closingHours ?: "") // טיפול ב-null
                putString("description", business.description ?: "") // טיפול ב-null
            }
            findNavController().navigate(
                R.id.action_businessDetailsFragment_to_secondSignUpFragment, bundle
            )

        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
