package com.example.gis_test

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.SignupSecPageBinding
import java.io.BufferedReader
import java.io.InputStreamReader

fun loadStreetsFromCsv(context: Context): List<String> {
    val streetsList = mutableListOf<String>()

    try {
        val inputStream = context.assets.open("TelAvivStreets.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            streetsList.add(line!!) // מוסיף את שם הרחוב
        }

        reader.close()
        inputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return streetsList
}

class SecondSignUpFragment : Fragment() {
    private var _binding: SignupSecPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = SignupSecPageBinding.inflate(inflater, container, false)
        val streets = loadStreetsFromCsv(requireContext())

        // יצירת Adapter עבור AutoCompleteTextView
        val streetAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)

        // הגדרת Adapter לשדה הרחוב
        binding.streetNameEdt.setAdapter(streetAdapter)
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
        val hours = arrayOf(
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23"
        )
        val minutes = arrayOf(
            "00", "15", "30", "45"
        )

        binding.openHourPicker.minValue = 0
        binding.openHourPicker.maxValue = hours.size - 1
        binding.openMinutePicker.minValue = 0
        binding.openMinutePicker.maxValue = minutes.size - 1
        binding.closeHourPicker.minValue = 0
        binding.closeHourPicker.maxValue = hours.size - 1
        binding.closeMinutePicker.minValue = 0
        binding.closeMinutePicker.maxValue = minutes.size - 1
        binding.openHourPicker.displayedValues = hours
        binding.openMinutePicker.displayedValues = minutes
        binding.closeHourPicker.displayedValues = hours
        binding.closeMinutePicker.displayedValues = minutes
        binding.categoryPicker.minValue = 0
        binding.categoryPicker.maxValue = (categories.size - 1)
        binding.categoryPicker.displayedValues = categories

        binding.signupBtn.setOnClickListener {

            val businessName = binding.businessNameEdt.text.toString()
            val businessCategory = categories[binding.categoryPicker.value]
            val businessStreet = binding.streetNameEdt.text.toString()
            val businessStreetNumber = binding.streetnumberEdt.text.toString()
            val businessOpeningHours = hours[binding.openHourPicker.value]
            val businessOpeningMinutes = minutes[binding.openMinutePicker.value]
            val businessClosingHours = hours[binding.closeHourPicker.value]
            val businessClosingMinutes = minutes[binding.closeMinutePicker.value]
            val businessDescription = binding.businessDescEdt.text.toString()

            // בדיקת ערכים ריקים
            if (businessName.isBlank() || businessCategory.isBlank() || businessStreet.isBlank()
                || businessStreetNumber.isBlank()
            ) {
                Toast.makeText(requireContext(), "יש למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (businessStreet.isNotEmpty() && businessStreetNumber.isNotEmpty()) {
                // ניתן להוסיף שמירה ב-SharedPreferences או בסיס נתונים מקומי
                val address = "$businessStreet $businessStreetNumber"
                println("Business Name: $businessName")
                println("Category: $businessCategory")
                println("Address: $address")
                println("Description: $businessDescription")

                // ניווט לדף הבא
                findNavController().navigate(R.id.action_secondSignUpFragment_to_myBusinessFragment)

            } else {
                Toast.makeText(
                    requireContext(),
                    "אנא מלא את שם הרחוב ומספר הבית.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
