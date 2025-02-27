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
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await
    import kotlinx.coroutines.withContext
    import okhttp3.OkHttpClient
    import okhttp3.Request
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

        private val auth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()
        private val client = OkHttpClient() // HTTP client for geocoding


        private val categories = arrayOf(
            "Restaurant", "Coffee place", "Beauty salon", "Grocery store",
            "Clothes store", "Book store", "Gym", "Pharmacy",
            "Hardware store", "Jewelry store"
        )
        private val hours = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
        private val minutes = arrayOf("00", "15", "30", "45")

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
            setupUI()
            setupListeners()
        }

        private fun setupUI() {
            val streets = loadStreetsFromCsv(requireContext())
            if (streets.isEmpty()) {
                Toast.makeText(requireContext(), "Failed to load streets data.", Toast.LENGTH_SHORT).show()
            }

            binding.streetNameEdt.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, streets)
            )

            // Configure pickers directly
            binding.openHourPicker.apply {
                minValue = 0
                maxValue = hours.size - 1
                displayedValues = hours
            }
            binding.openMinutePicker.apply {
                minValue = 0
                maxValue = minutes.size - 1
                displayedValues = minutes
            }
            binding.closeHourPicker.apply {
                minValue = 0
                maxValue = hours.size - 1
                displayedValues = hours
            }
            binding.closeMinutePicker.apply {
                minValue = 0
                maxValue = minutes.size - 1
                displayedValues = minutes
            }
            binding.categoryPicker.apply {
                minValue = 0
                maxValue = categories.size - 1
                displayedValues = categories
            }
        }

        private fun setupListeners() {
            binding.signupBtn.setOnClickListener {
                signUpBusinessAndUser()
            }
        }

        private fun signUpBusinessAndUser() {
            var userName: String? = null
            var email: String? = null
            var password: String? = null

            //   Extract user details from arguments
            arguments?.let {
                userName = it.getString("userName")
                email = it.getString("userEmail")
                password = it.getString("userPassword")
            }

            lifecycleScope.launch {
                try {
                    val businessName = binding.businessNameEdt.text.toString().trim()
                    val streetNumber = binding.streetnumberEdt.text.toString().trim()
                    val street = binding.streetNameEdt.text.toString().trim()

                    //   Ensure all fields are filled
                    if (businessName.isEmpty() || street.isEmpty() || streetNumber.isEmpty() ||
                        userName.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    //   Disable button while registering
                    binding.signupBtn.isEnabled = false
                    binding.signupBtn.text = "Registering..."

                    //   Check if the user already exists in Firestore
                    val existingUser = firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get().await()

                    val userId: String

                    if (existingUser.isEmpty) {
                        //   Register the user in Firebase Authentication
                        val authResult = auth.createUserWithEmailAndPassword(email!!, password!!).await()
                        val firebaseUser = authResult.user

                        if (firebaseUser == null) {
                            Toast.makeText(requireContext(), "User registration failed.", Toast.LENGTH_SHORT).show()
                            binding.signupBtn.isEnabled = true
                            binding.signupBtn.text = "Sign Up"
                            return@launch
                        }

                        userId = firebaseUser.uid

                        //   Save new user in Firestore with userName
                        val userRef = firestore.collection("users").document(userId)
                        val userData = hashMapOf(
                            "userId" to userId,
                            "userName" to userName, //   Save user name
                            "email" to email,
                            "password" to password // ⚠️ Ideally, do NOT store passwords in plaintext
                        )
                        userRef.set(userData).await()
                        Log.d("BusinessSignUpFragment", "User registered: $userName ($email)")
                    } else {
                        //   Use existing user ID
                        userId = existingUser.documents[0].id
                    }

                    //   Get business coordinates
                    val address = "$streetNumber $street, Tel Aviv, Israel"
                    val coordinates = getCoordinatesFromAddress(address)
                    if (coordinates == null) {
                        Toast.makeText(requireContext(), "Could not fetch location, try again.", Toast.LENGTH_SHORT).show()
                        binding.signupBtn.isEnabled = true
                        binding.signupBtn.text = "Sign Up"
                        return@launch
                    }
                    val (latitude, longitude) = coordinates

                    //   Save business to Firestore
                    //   Save business to Firestore with all necessary fields
                    val businessRef = firestore.collection("businesses").document()
                    val businessData = hashMapOf(
                        "businessIdFirestore" to businessRef.id,
                        "name" to businessName,
                        "category" to categories[binding.categoryPicker.value], //   Ensure category is stored
                        "street" to street,
                        "streetNumber" to streetNumber,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "openingHours" to "${hours[binding.openHourPicker.value]}:${minutes[binding.openMinutePicker.value]}", //   Opening hours
                        "closingHours" to "${hours[binding.closeHourPicker.value]}:${minutes[binding.closeMinutePicker.value]}", //   Closing hours
                        "description" to binding.businessDescEdt.text.toString().trim(), //   Business description
                        "userId" to userId //   Link business to user
                    )

    //   Save business data to Firestore
                    businessRef.set(businessData).await()


                    Log.d("BusinessSignUpFragment", "Business saved: $businessName → ($latitude, $longitude)")

                    Toast.makeText(requireContext(), "Business registered successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)

                } catch (e: Exception) {
                    Log.e("BusinessSignUpFragment", "Error registering user or business", e)
                    Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.signupBtn.isEnabled = true
                    binding.signupBtn.text = "Sign Up"
                }
            }
        }





        private suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? {
            return withContext(Dispatchers.IO) {
                try {
                    val apiKey = "AIzaSyCQeEvjm6akDFJ78wVUY6pqG1tuBUp1Yyw" // 🔴 Replace with your actual API key
                    val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()

                    response.body?.string()?.let { responseBody ->
                        val jsonResponse = org.json.JSONObject(responseBody)
                        val results = jsonResponse.getJSONArray("results")

                        if (results.length() > 0) {
                            val location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")

                            val lat = location.getDouble("lat")
                            val lon = location.getDouble("lng")
                            Log.d("Geocoding", "Geocoded: $address → $lat, $lon")
                            return@withContext Pair(lat, lon)
                        } else {
                            Log.e("Geocoding", "No results for $address")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Geocoding", "Google Geocoding error: ${e.message}")
                }
                return@withContext null
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
