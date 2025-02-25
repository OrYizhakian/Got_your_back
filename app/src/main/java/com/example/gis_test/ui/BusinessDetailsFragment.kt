package com.example.gis_test.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.GotYourBack.R
import com.example.GotYourBack.databinding.BusinessDetailsBinding
import com.example.gis_test.data.Business
import com.google.firebase.firestore.FirebaseFirestore

class BusinessDetailsFragment : Fragment() {

    private var _binding: BusinessDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var business: Business
    private val firestore = FirebaseFirestore.getInstance() // ✅ Firestore instance for fetching updates

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BusinessDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        business = arguments?.getParcelable("business") ?: return

        if (business.businessIdFirestore.isNotEmpty()) {
            Log.d("BusinessDetailsFragment", "Firestore ID: ${business.businessIdFirestore}")
            fetchUpdatedBusinessData(business.businessIdFirestore) // ✅ Fetch fresh data
        } else {
            Log.e("BusinessDetailsFragment", "Business data is missing!")
            Toast.makeText(requireContext(), "Error loading business details", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun fetchUpdatedBusinessData(firestoreId: String) {
        firestore.collection("businesses").document(firestoreId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val updatedBusiness = Business(
                        businessIdFirestore = document.id,
                        userId = document.getString("userId") ?: "",
                        name = document.getString("name") ?: "",
                        category = document.getString("category") ?: "",
                        street = document.getString("street") ?: "",
                        streetNumber = document.getString("streetNumber") ?: "",
                        openingHours = document.getString("openingHours") ?: "",
                        closingHours = document.getString("closingHours") ?: "",
                        description = document.getString("description") ?: ""
                    )

                    Log.d("BusinessDetailsFragment", "Updated business data: $updatedBusiness")
                    displayBusinessDetails(updatedBusiness) // ✅ Update UI with fresh data
                } else {
                    Log.e("BusinessDetailsFragment", "Document not found in Firestore")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("BusinessDetailsFragment", "Error fetching updated business", exception)
            }
    }

    private fun displayBusinessDetails(business: Business) {
        binding.apply {
            businessNameText.text = business.name
            businessCategoryText.text = business.category
            businessAddressText.text = "${business.street} ${business.streetNumber}"
            businessHoursText.text = "${business.openingHours} - ${business.closingHours}"
            businessDescriptionText.text = business.description ?: "No description provided"
        }

        // ✅ Enable edit button & set click listener
        binding.editButton.isEnabled = true
        binding.editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("business", business) // ✅ Pass business object for editing
            }
            Log.d("BusinessDetailsFragment", "Navigating to Edit Business: ${business.businessIdFirestore}")

            // ✅ Ensure correct ID in nav_graph.xml
            findNavController().navigate(
                R.id.action_businessDetailsFragment2_to_businessUpdateFragment, bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
