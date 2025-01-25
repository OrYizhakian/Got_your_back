package com.example.gis_test.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.gis_test.data.User
import com.example.gis_test.databinding.BusinessSignupPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var _binding: BusinessSignupPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var categories: Array<String>
    private lateinit var hours: Array<String>
    private lateinit var minutes: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BusinessSignupPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load streets data
        val streets = loadStreetsFromCsv(requireContext())
        if (streets.isEmpty()) {
            Toast.makeText(requireContext(), "Failed to load streets data.", Toast.LENGTH_SHORT).show()
        }

        binding.streetNameEdt.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)
        )

        // Load categories and time pickers
        categories = arrayOf(
            "Restaurant",
            "Coffee place",
            "Beauty saloon",
            "Grocery store",
            "Clothes store",
            "Book store",
            "Gym",
            "Pharmacy",
            "Hardware store",
            "Jewelry store"
        )
        hours = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
        minutes = arrayOf("00", "15", "30", "45")

        setupPicker(binding.openHourPicker, hours)
        setupPicker(binding.openMinutePicker, minutes)
        setupPicker(binding.closeHourPicker, hours)
        setupPicker(binding.closeMinutePicker, minutes)
        setupPicker(binding.categoryPicker, categories)

        // Load arguments if present
        arguments?.let { args ->
            binding.businessNameEdt.setText(args.getString("name", ""))
            binding.streetNameEdt.setText(args.getString("street", ""))
            binding.streetnumberEdt.setText(args.getString("streetNumber", ""))
            binding.businessDescEdt.setText(args.getString("description", ""))

            val openingHours = args.getString("openingHours", "00:00").split(":")
            binding.openHourPicker.value = openingHours[0].toInt()
            binding.openMinutePicker.value = openingHours[1].toInt()

            val closingHours = args.getString("closingHours", "00:00").split(":")
            binding.closeHourPicker.value = closingHours[0].toInt()
            binding.closeMinutePicker.value = closingHours[1].toInt()

            val categoryIndex = categories.indexOf(args.getString("category", categories[0]))
            binding.categoryPicker.value = if (categoryIndex >= 0) categoryIndex else 0
        }

        // Save button listener
        binding.signupBtn.setOnClickListener {
            saveBusiness()
        }
    }

    private fun saveBusiness() {
        val businessName = binding.businessNameEdt.text.toString().trim()
        val businessStreetNumber = binding.streetnumberEdt.text.toString().trim()
        val businessStreet = binding.streetNameEdt.text.toString().trim()
        val businessCategory = categories[binding.categoryPicker.value]
        val openingHours =
            "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}"
        val closingHours =
            "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}"
        val businessDescription = binding.businessDescEdt.text.toString().trim()

        if (!isInputValid(businessName, businessStreet, businessStreetNumber)) {
            Toast.makeText(requireContext(), "Please fill in all the fields", Toast.LENGTH_SHORT)
                .show()
            return
        }

        lifecycleScope.launch {
            try {
                // תחילת הפעולה בקורוטינה

                // הוספת משתמש
                val userName = arguments?.getString("userName", "") ?: ""
                val userEmail = arguments?.getString("userEmail", "") ?: ""
                val userPassword = arguments?.getString("userPassword", "") ?: ""

                val user = User(
                    userName = userName,
                    email = userEmail,
                    password = userPassword
                )

                // הוספת המשתמש למסד הנתונים
                val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                val userId = userDao.insertUser(user)

                // הוספת עסק
                val business = Business(
                    userId = userId,
                    businessId = 0L, // ID של העסק אם הוא נוצר אוטומטית
                    name = businessName,
                    category = businessCategory,
                    street = businessStreet,
                    streetNumber = businessStreetNumber,
                    openingHours = openingHours,
                    closingHours = closingHours,
                    description = businessDescription
                )

                val businessDao = AppDatabase.getDatabase(requireContext()).businessDao()
                businessDao.insertBusiness(business)

                Toast.makeText(requireContext(), "Business saved successfully!", Toast.LENGTH_SHORT).show()

                // מעבר למסך הבא
                findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)

            } catch (e: Exception) {
                // אם יש שגיאה בשמירה
                Toast.makeText(requireContext(), "Failed to update business.", Toast.LENGTH_SHORT).show()
                Log.e("SecondSignUpFragment", "Error saving business", e)
            }
        }
    }



    private fun setupPicker(picker: android.widget.NumberPicker, values: Array<String>) {
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
