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
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
        categories = arrayOf(
            "Restaurant", "Coffee place", "Beauty salon", "Grocery store",
            "Clothes store", "Book store", "Gym", "Pharmacy",
            "Hardware store", "Jewelry store"
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
    }

    private fun setupListeners() {
        binding.signupBtn.setOnClickListener {
            signUpBusinessAndUser()
        }
    }

    private fun signUpBusinessAndUser() {
        lifecycleScope.launch {
            try {
                // ✅ Get user input directly
                val businessName = binding.businessNameEdt.text.toString().trim()
                val streetNumber = binding.streetnumberEdt.text.toString().trim()
                val street = binding.streetNameEdt.text.toString().trim()
                val category = categories[binding.categoryPicker.value]
                val openingHours = "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}"
                val closingHours = "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}"
                val description = binding.businessDescEdt.text.toString().trim()

                // ✅ Validate required fields
                if (businessName.isEmpty() || street.isEmpty() || streetNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ Firebase authentication (create user)
                val userName = arguments?.getString("userName") ?: throw Exception("Username is missing")
                val userEmail = arguments?.getString("userEmail") ?: throw Exception("Email is missing")
                val userPassword = arguments?.getString("userPassword") ?: throw Exception("Password is missing")

                val authResult = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(userEmail, userPassword).await()
                }
                val firebaseUserId = authResult.user?.uid ?: throw Exception("Failed to create Firebase user")

                Log.d("BusinessSignUpFragment", "Created Firebase user with ID: $firebaseUserId")

                // ✅ Save user in local Room database
                val localUserId = withContext(Dispatchers.IO) {
                    val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                    userDao.insertUser(
                        User(userName = userName, email = userEmail, password = userPassword, fireBaseId = firebaseUserId)
                    )
                }

                Log.d("BusinessSignUpFragment", "Created local user with ID: $localUserId")

                // ✅ Generate Firestore document reference first
                val businessRef = firestore.collection("businesses").document() // 🔥 Generates a document with an ID
                val firestoreBusinessId = businessRef.id // ✅ Get Firestore document ID

                // ✅ Save business to Firestore (WITH the Firestore ID included)
                val businessMap = hashMapOf(
                    "businessIdFirestore" to firestoreBusinessId, // ✅ Store Firestore ID at creation
                    "userId" to firebaseUserId,
                    "name" to businessName,
                    "category" to category,
                    "street" to street,
                    "streetNumber" to streetNumber,
                    "openingHours" to openingHours,
                    "closingHours" to closingHours,
                    "description" to description
                )

                withContext(Dispatchers.IO) {
                    businessRef.set(businessMap).await() // ✅ Set Firestore document WITH ID
                }

                Log.d("BusinessSignUpFragment", "Saved business to Firestore with ID: $firestoreBusinessId")

                // ✅ Save business in Room database
                val business = Business(
                    businessIdFirestore = firestoreBusinessId, // ✅ Now it's correctly stored
                    userId = localUserId.toString(),
                    name = businessName,
                    category = category,
                    street = street,
                    streetNumber = streetNumber,
                    openingHours = openingHours,
                    closingHours = closingHours,
                    description = description
                )

                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).businessDao().insertBusiness(business)
                }

                Log.d("BusinessSignUpFragment", "Saved business to Room with Firestore ID: $firestoreBusinessId")

                // ✅ Navigate to login
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Business registered successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)
                }

            } catch (e: Exception) {
                Log.e("BusinessSignUpFragment", "Error during business registration", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun setupPicker(picker: android.widget.NumberPicker, values: Array<String>) {
        picker.minValue = 0
        picker.maxValue = values.size - 1
        picker.displayedValues = values
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
