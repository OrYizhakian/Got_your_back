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
import com.example.GotYourBack.R
import com.example.GotYourBack.databinding.BusinessSignupPageBinding
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.gis_test.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
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

class BusinessSignUpFragment : Fragment() {
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
        val openingHours = "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}"
        val closingHours = "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}"
        val businessDescription = binding.businessDescEdt.text.toString().trim()

        if (!isInputValid(businessName, businessStreet, businessStreetNumber)) {
            Toast.makeText(requireContext(), "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val userName = arguments?.getString("userName", "") ?: ""
                val userEmail = arguments?.getString("userEmail", "") ?: ""
                val userPassword = arguments?.getString("userPassword", "") ?: ""

                val auth = FirebaseAuth.getInstance()

                auth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid ?: return@addOnCompleteListener


                            // ✅ Save user to Firebase Firestore
                            val userMap = hashMapOf(
                                "uName" to userName,
                                "email" to userEmail,
                                "password" to userPassword
                            )

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    println("User registered successfully")

                                    // ✅ Save Business to Firestore
                                    val businessMap = hashMapOf(
                                        "userId" to userId, // Link business to user
                                        "name" to businessName,
                                        "category" to businessCategory,
                                        "street" to businessStreet,
                                        "streetNumber" to businessStreetNumber,
                                        "openingHours" to openingHours,
                                        "closingHours" to closingHours,
                                        "description" to businessDescription
                                    )

                                    FirebaseFirestore.getInstance()
                                        .collection("businesses")
                                        .add(businessMap)
                                        .addOnSuccessListener {
                                            println("Business saved successfully")
                                            // ✅ Save user to Room Database
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                                                val localUserId = userDao.insertUser(
                                                    User(userName = userName, email = userEmail, password = userPassword, fireBaseId = userId)
                                                )

                                                // ✅ Save business to Room Database
                                                val businessDao = AppDatabase.getDatabase(requireContext()).businessDao()
                                                val localBusiness = Business(
                                                    userId = localUserId, // Local DB user ID
                                                    businessId = 0L, // Auto-increment
                                                    name = businessName,
                                                    category = businessCategory,
                                                    street = businessStreet,
                                                    streetNumber = businessStreetNumber,
                                                    openingHours = openingHours,
                                                    closingHours = closingHours,
                                                    description = businessDescription
                                                )
                                                businessDao.insertBusiness(localBusiness)
                                            }
                                            // Navigate only after everything is saved
                                            findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)
                                        }
                                        .addOnFailureListener { e ->
                                            println("Failed to save business: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    println("Failed to save user: ${e.message}")
                                }
                        } else {
                            println("Registration failed: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
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
