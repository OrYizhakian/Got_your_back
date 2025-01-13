package com.example.gis_test.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gis_test.databinding.MyBusinessScreenBinding

class MyBusinessFragment : Fragment() {
    private var _binding: MyBusinessScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MyBusinessScreenBinding.inflate(inflater, container, false)

        // קבלת הנתונים מתוך ה-Bundle
        val businessName = arguments?.getString("businessName") ?: "N/A"
        val businessCategory = arguments?.getString("businessCategory") ?: "N/A"
        val businessStreet = arguments?.getString("businessStreet") ?: "N/A"
        val businessStreetNumber = arguments?.getString("businessStreetNumber") ?: "N/A"
        val businessOpeningHours = arguments?.getString("businessOpeningHours") ?: "N/A"
        val businessOpeningMinutes = arguments?.getString("businessOpeningMinutes") ?: "N/A"
        val businessClosingHours = arguments?.getString("businessClosingHours") ?: "N/A"
        val businessClosingMinutes = arguments?.getString("businessClosingMinutes") ?: "N/A"
        val businessDescription = arguments?.getString("businessDescription") ?: "N/A"

        // הצגת הנתונים על המסך
        binding.businessNameText.text = "Business Name: $businessName"
        binding.businessCategoryText.text = "Category: $businessCategory"
        binding.businessAddressText.text = "Address: $businessStreet $businessStreetNumber"
        binding.businessHoursText.text =
            "Hours: $businessOpeningHours:$businessOpeningMinutes - $businessClosingHours:$businessClosingMinutes"
        binding.businessDescriptionText.text = "Description: $businessDescription"

        // פעולה לחצן "Back"
        binding.mrsBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
