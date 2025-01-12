package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels


import com.example.gis_test.databinding.MyBusinessScreenBinding

class MyBusinessFragment : Fragment() {
    private var _binding: MyBusinessScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels() // קישור ל-ViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MyBusinessScreenBinding.inflate(inflater, container, false)

        // קבלת הנתונים מה-ViewModel
        val user = viewModel.user.value
        if (user != null) {
            binding.businessNameText.text = "Business Name: ${user.businessName}"
            binding.businessCategoryText.text = "Category: ${user.businessCategory}"
            binding.businessAddressText.text =
                "Address: ${user.businessStreet} ${user.businessStreetNumber}"
            binding.businessHoursText.text =
                "Hours: ${user.businessOpeningHours}:${user.businessOpeningMinutes} - ${user.businessClosingHours}:${user.businessClosingMinutes}"
            binding.businessDescriptionText.text = "Description: ${user.businessDescription}"
        } else {
            // הצגת הודעה במקרה שאין נתונים
            binding.businessNameText.text = "No business details available."
        }

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
