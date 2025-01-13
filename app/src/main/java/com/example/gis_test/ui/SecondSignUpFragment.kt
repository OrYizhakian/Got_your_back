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
import androidx.navigation.fragment.findNavController
import com.example.gis_test.R
import com.example.gis_test.databinding.SignupSecPageBinding
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

        val streets = loadStreetsFromCsv(requireContext())
        if (streets.isEmpty()) {
            Toast.makeText(requireContext(), "Failed to load streets data.", Toast.LENGTH_SHORT).show()
        }

        binding.streetNameEdt.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)
        )

        val categories = arrayOf(
            "מסעדה",
            "בית קפה",
            "סלון יופי",
            "מכולת",
            "חנות בגדים",
            "חנות ספרים",
            "מכון כושר",
            "בית מרקחת",
            "חנות חומרי בניין",
            "חנות תכשיטים"
        )

        val hours = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
        val minutes = arrayOf("00", "15", "30", "45")

        setupPicker(binding.openHourPicker, hours)
        setupPicker(binding.openMinutePicker, minutes)
        setupPicker(binding.closeHourPicker, hours)
        setupPicker(binding.closeMinutePicker, minutes)
        setupPicker(binding.categoryPicker, categories)

//        binding.openHourPicker.minValue = 0
//        binding.openHourPicker.maxValue = hours.size - 1
//        binding.openMinutePicker.minValue = 0
//        binding.openMinutePicker.maxValue = minutes.size - 1
//        binding.closeHourPicker.minValue = 0
//        binding.closeHourPicker.maxValue = hours.size - 1
//        binding.closeMinutePicker.minValue = 0
//        binding.closeMinutePicker.maxValue = minutes.size - 1
//        binding.openHourPicker.displayedValues = hours
//        binding.openMinutePicker.displayedValues = minutes
//        binding.closeHourPicker.displayedValues = hours
//        binding.closeMinutePicker.displayedValues = minutes
//        binding.categoryPicker.minValue = 0
//        binding.categoryPicker.maxValue = (categories.size - 1)
//        binding.categoryPicker.displayedValues = categories

        binding.signupBtn.setOnClickListener {

            val businessName = binding.businessNameEdt.text.toString().trim()
            //val businessCategory = categories[binding.categoryPicker.value]
            val businessStreet = binding.streetNameEdt.text.toString().trim()
            val businessStreetNumber = binding.streetnumberEdt.text.toString().trim()
            //val businessOpeningHours = hours[binding.openHourPicker.value]
            //val businessOpeningMinutes = minutes[binding.openMinutePicker.value]
            //val businessClosingHours = hours[binding.closeHourPicker.value]
            //val businessClosingMinutes = minutes[binding.closeMinutePicker.value]
            //val businessDescription = binding.businessDescEdt.text.toString()

            if (!isInputValid(businessName, businessStreet, businessStreetNumber)) {
                Toast.makeText(requireContext(), "יש למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prepare bundle with data
            val args = Bundle().apply {
                putString("businessName", businessName)
                putString("businessCategory", categories[binding.categoryPicker.value])
                putString("businessStreet", businessStreet)
                putString("businessStreetNumber", businessStreetNumber)
                putString("businessOpeningHours", hours[binding.openHourPicker.value])
                putString("businessOpeningMinutes", minutes[binding.openMinutePicker.value])
                putString("businessClosingHours", hours[binding.closeHourPicker.value])
                putString("businessClosingMinutes", minutes[binding.closeMinutePicker.value])
                putString("businessDescription", binding.businessDescEdt.text.toString().trim())
            }

            // Navigate to MyBusinessFragment with arguments
            findNavController().navigate(
                R.id.action_secondSignUpFragment_to_myBusinessFragment,
                args
            )
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
