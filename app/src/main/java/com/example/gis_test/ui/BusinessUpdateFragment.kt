package com.example.gis_test.ui

import android.R
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.GotYourBack.databinding.BusinessEditPageBinding
import com.example.gis_test.data.Business
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.InputStreamReader

class BusinessUpdateFragment : Fragment() {

    private var _binding: BusinessEditPageBinding? = null
    private val binding get() = _binding!!
    private var business: Business? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BusinessEditPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        business = arguments?.getParcelable("business") // ✅ Retrieve business

        if (business != null) {
            populateFields(business!!) // ✅ Fill fields with existing data
        } else {
            Toast.makeText(requireContext(), "Error: Business not found.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        setupListeners()
    }

    private fun loadStreetsFromCsv(context: Context): List<String> {
        return try {
            context.assets.open("TelAvivStreets.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().toList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun populateFields(business: Business) {
        val streets = loadStreetsFromCsv(requireContext())

        binding.streetNameEdt.setAdapter(
            ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, streets)
        )
        binding.businessNameEdt.setText(business.name)
        binding.streetNameEdt.setText(business.street)
        binding.streetnumberEdt.setText(business.streetNumber)
        binding.businessDescEdt.setText(business.description ?: "")

        // Set category picker options
        val categories = arrayOf(
            "Restaurant", "Coffee place", "Beauty salon", "Grocery store",
            "Clothes store", "Book store", "Gym", "Pharmacy",
            "Hardware store", "Jewelry store"
        )

        val categoryIndex = categories.indexOf(business.category).takeIf { it >= 0 } ?: 0
        binding.categoryPicker.minValue = 0
        binding.categoryPicker.maxValue = categories.size - 1
        binding.categoryPicker.displayedValues = categories
        binding.categoryPicker.value = categoryIndex

        // Set opening and closing hours
        val hours = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
        val minutes = arrayOf("00", "15", "30", "45")

        val openHour = business.openingHours.substringBefore(":").toIntOrNull() ?: 0
        val openMinute = business.openingHours.substringAfter(":").toIntOrNull() ?: 0
        val closeHour = business.closingHours.substringBefore(":").toIntOrNull() ?: 0
        val closeMinute = business.closingHours.substringAfter(":").toIntOrNull() ?: 0

        binding.openHourPicker.minValue = 0
        binding.openHourPicker.maxValue = hours.size - 1
        binding.openHourPicker.displayedValues = hours
        binding.openHourPicker.value = openHour

        binding.openMinutePicker.minValue = 0
        binding.openMinutePicker.maxValue = minutes.size - 1
        binding.openMinutePicker.displayedValues = minutes
        binding.openMinutePicker.value = minutes.indexOf(openMinute.toString().padStart(2, '0'))

        binding.closeHourPicker.minValue = 0
        binding.closeHourPicker.maxValue = hours.size - 1
        binding.closeHourPicker.displayedValues = hours
        binding.closeHourPicker.value = closeHour

        binding.closeMinutePicker.minValue = 0
        binding.closeMinutePicker.maxValue = minutes.size - 1
        binding.closeMinutePicker.displayedValues = minutes
        binding.closeMinutePicker.value = minutes.indexOf(closeMinute.toString().padStart(2, '0'))
    }

    private fun setupListeners() {
        binding.updateBtn.setOnClickListener {
            updateBusinessInFirestore()
        }
    }

    private fun updateBusinessInFirestore() {
        val businessId = business?.businessIdFirestore

        // ✅ Ensure businessIdFirestore is valid
        if (businessId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: Business ID is missing.", Toast.LENGTH_SHORT).show()
            Log.e("BusinessUpdateFragment", "Error: businessIdFirestore is null or empty")
            return
        }

        val updatedBusiness = hashMapOf(
            "name" to binding.businessNameEdt.text.toString().trim(),
            "category" to binding.categoryPicker.displayedValues[binding.categoryPicker.value],
            "street" to binding.streetNameEdt.text.toString().trim(),
            "streetNumber" to binding.streetnumberEdt.text.toString().trim(),
            "openingHours" to "${binding.openHourPicker.displayedValues[binding.openHourPicker.value]}:${binding.openMinutePicker.displayedValues[binding.openMinutePicker.value]}",
            "closingHours" to "${binding.closeHourPicker.displayedValues[binding.closeHourPicker.value]}:${binding.closeMinutePicker.displayedValues[binding.closeMinutePicker.value]}",
            "description" to binding.businessDescEdt.text.toString().trim()
        )

        val firestore = FirebaseFirestore.getInstance()

        // ✅ Ensure the document reference is valid
        val businessRef = firestore.collection("businesses").document(businessId)

        businessRef.update(updatedBusiness as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Business updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // ✅ Navigate back after success
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("BusinessUpdateFragment", "Error updating business", e)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
