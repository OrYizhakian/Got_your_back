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
import kotlinx.coroutines.tasks.await
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

        setupInitialData()
        setupUI()
        setupListeners()
    }

    private fun setupInitialData() {
        // Initialize arrays
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
    }

    private fun setupUI() {
        // Setup street data
        val streets = loadStreetsFromCsv(requireContext())
        if (streets.isEmpty()) {
            Toast.makeText(requireContext(), "Failed to load streets data.", Toast.LENGTH_SHORT).show()
        }

        binding.streetNameEdt.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)
        )

        // Setup pickers
        setupPicker(binding.openHourPicker, hours)
        setupPicker(binding.openMinutePicker, minutes)
        setupPicker(binding.closeHourPicker, hours)
        setupPicker(binding.closeMinutePicker, minutes)
        setupPicker(binding.categoryPicker, categories)

        // Load any existing data from arguments
        loadArgumentData()
    }

    private fun loadArgumentData() {
        arguments?.let { args ->
            binding.apply {
                businessNameEdt.setText(args.getString("name", ""))
                streetNameEdt.setText(args.getString("street", ""))
                streetnumberEdt.setText(args.getString("streetNumber", ""))
                businessDescEdt.setText(args.getString("description", ""))

                args.getString("openingHours", "00:00").split(":").let { time ->
                    openHourPicker.value = time[0].toIntOrNull() ?: 0
                    openMinutePicker.value = time[1].toIntOrNull() ?: 0
                }

                args.getString("closingHours", "00:00").split(":").let { time ->
                    closeHourPicker.value = time[0].toIntOrNull() ?: 0
                    closeMinutePicker.value = time[1].toIntOrNull() ?: 0
                }

                val categoryIndex = categories.indexOf(args.getString("category", categories[0]))
                categoryPicker.value = if (categoryIndex >= 0) categoryIndex else 0
            }
        }
    }

    private fun setupListeners() {
        binding.signupBtn.setOnClickListener {
            saveBusiness()
        }
    }

    private fun saveBusiness() {
        // Collect all business data
        val businessData = collectBusinessData()

        if (!validateBusinessData(businessData)) {
            return
        }

        lifecycleScope.launch {
            try {
                val userName = arguments?.getString("userName") ?: throw Exception("Username is missing")
                val userEmail = arguments?.getString("userEmail") ?: throw Exception("Email is missing")
                val userPassword = arguments?.getString("userPassword") ?: throw Exception("Password is missing")

                // Step 1: Create Firebase User
                val auth = FirebaseAuth.getInstance()
                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(userEmail, userPassword).await()
                }
                val firebaseUserId = authResult.user?.uid ?: throw Exception("Failed to create Firebase user")

                // Step 2: Create User in Room Database
                val localUserId = withContext(Dispatchers.IO) {
                    val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                    userDao.insertUser(
                        User(
                            userName = userName,
                            email = userEmail,
                            password = userPassword,
                            fireBaseId = firebaseUserId
                        )
                    )
                }

                // Step 3: Save User to Firestore
                val userMap = hashMapOf(
                    "uName" to userName,
                    "email" to userEmail,
                    "password" to userPassword
                )

                withContext(Dispatchers.IO) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUserId)
                        .set(userMap)
                        .await()
                }

                // Step 4: Create Business in Room Database
                val business = Business(
                    userId = localUserId, // Using the local Room database user ID
                    name = businessData.name,
                    category = businessData.category,
                    street = businessData.street,
                    streetNumber = businessData.streetNumber,
                    openingHours = businessData.openingHours,
                    closingHours = businessData.closingHours,
                    description = businessData.description
                )

                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext())
                        .businessDao()
                        .insertBusiness(business)
                }

                // Step 5: Save Business to Firestore
                val businessMap = hashMapOf(
                    "userId" to firebaseUserId,
                    "name" to businessData.name,
                    "category" to businessData.category,
                    "street" to businessData.street,
                    "streetNumber" to businessData.streetNumber,
                    "openingHours" to businessData.openingHours,
                    "closingHours" to businessData.closingHours,
                    "description" to businessData.description
                )

                withContext(Dispatchers.IO) {
                    FirebaseFirestore.getInstance()
                        .collection("businesses")
                        .add(businessMap)
                        .await()
                }

                // Success - navigate to login
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)
                }

            } catch (e: Exception) {
                Log.e("BusinessSignUpFragment", "Error saving business", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to register: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun collectBusinessData(): BusinessData {
        return BusinessData(
            name = binding.businessNameEdt.text.toString().trim(),
            streetNumber = binding.streetnumberEdt.text.toString().trim(),
            street = binding.streetNameEdt.text.toString().trim(),
            category = categories[binding.categoryPicker.value],
            openingHours = "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}",
            closingHours = "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}",
            description = binding.businessDescEdt.text.toString().trim()
        )
    }

    private fun validateBusinessData(data: BusinessData): Boolean {
        if (!isInputValid(data.name, data.street, data.streetNumber)) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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

// Data class to hold business information
private data class BusinessData(
    val name: String,
    val streetNumber: String,
    val street: String,
    val category: String,
    val openingHours: String,
    val closingHours: String,
    val description: String
)