package com.example.gis_test.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.gis_test.databinding.SignupSecPageBinding
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

fun loadStreetsFromCsv(context: Context): List<String> {
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

class SecondSignUpFragment : Fragment() {
    private var _binding: SignupSecPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SignupSecPageBinding.inflate(inflater, container, false)

        // Retrieve userId from arguments
        val userId = arguments?.getLong("userId") ?: -1L
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Error: User ID is missing.", Toast.LENGTH_SHORT)
                .show()
            findNavController().popBackStack() // Navigate back to the previous screen
            return binding.root
        }

        val streets = loadStreetsFromCsv(requireContext())
        if (streets.isEmpty()) {
            Toast.makeText(requireContext(), "Failed to load streets data.", Toast.LENGTH_SHORT)
                .show()
        }

        binding.streetNameEdt.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)
        )

        val categories = arrayOf(
            "Restaurant",
            "Coffee place",
            "Beauty saloon",
            "Grocery store",
            "Clothes store",
            "Book store",
            "Gym",
            "Pharmacy",
            "Hardware store",
            "Jewelery store"
        )

        val hours = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
        val minutes = arrayOf("00", "15", "30", "45")

        setupPicker(binding.openHourPicker, hours)
        setupPicker(binding.openMinutePicker, minutes)
        setupPicker(binding.closeHourPicker, hours)
        setupPicker(binding.closeMinutePicker, minutes)
        setupPicker(binding.categoryPicker, categories)

        binding.signupBtn.setOnClickListener {

            val businessName = binding.businessNameEdt.text.toString().trim()
            val businessStreetNumber = binding.streetnumberEdt.text.toString().trim()

            val businessStreet = binding.streetNameEdt.text.toString().trim()
            if (businessStreet !in streets) {
                binding.streetNameEdt.text.clear()
                Toast.makeText(
                    requireContext(),
                    "Please pick a street from the list.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener // Exit early if the street is invalid
            }

            if (!isInputValid(businessName, businessStreet, businessStreetNumber)) {
                Toast.makeText(
                    requireContext(),
                    "Please fill in all the fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val businessCategory = categories[binding.categoryPicker.value]
            val openingHours =
                "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}"
            val closingHours =
                "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}"
            val businessDescription = binding.businessDescEdt.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val business = Business(
                        userId = userId,
                        name = businessName,
                        category = businessCategory,
                        street = businessStreet,
                        streetNumber = businessStreetNumber,
                        openingHours = openingHours,
                        closingHours = closingHours,
                        description = businessDescription
                    )

                    AppDatabase.getDatabase(requireContext()).businessDao().insertBusiness(business)

                    Toast.makeText(
                        requireContext(),
                        "Business saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to the next fragment or finish the flow
                    findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save business. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return binding.root
    }

    private fun setupPicker(picker: NumberPicker, values: Array<String>) {
        picker.minValue = 0
        picker.maxValue = values.size - 1
        picker.displayedValues = values
    }

    private fun isInputValid(vararg inputs: String): Boolean {
        return inputs.all { it.isNotBlank() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
