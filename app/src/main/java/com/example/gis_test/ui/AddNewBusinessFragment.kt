package com.example.gis_test.ui

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
import com.example.GotYourBack.databinding.BusinessAddPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AddNewBusinessFragment : Fragment() {
    private var _binding: BusinessAddPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var categories: Array<String>
    private lateinit var hours: Array<String>
    private lateinit var minutes: Array<String>

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient() // HTTP client for geocoding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BusinessAddPageBinding.inflate(inflater, container, false)
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
            "Beauty salon",
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
        binding.addBtn.setOnClickListener {
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

        if (businessName.isEmpty() || businessStreet.isEmpty() || businessStreetNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("AddNewBusinessFragment", "Saving business: $businessName, $businessStreet, $businessCategory, $openingHours, $closingHours")

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val address = "$businessStreetNumber $businessStreet, Tel Aviv, Israel"
                val coordinates = getCoordinatesFromAddress(address)
                if (coordinates == null) {
                    Toast.makeText(requireContext(), "Could not fetch location, try again.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val (latitude, longitude) = coordinates

                val businessRef = firestore.collection("businesses").document()
                val businessMap = hashMapOf(
                    "businessIdFirestore" to businessRef.id,  //    Unique Firestore ID
                    "userId" to currentUser.uid,
                    "name" to businessName,
                    "category" to businessCategory,
                    "street" to businessStreet,
                    "streetNumber" to businessStreetNumber,
                    "openingHours" to openingHours,
                    "closingHours" to closingHours,
                    "description" to businessDescription,
                    "latitude" to latitude,  //    Ensure location is stored
                    "longitude" to longitude
                )

                withContext(Dispatchers.IO) {
                    businessRef.set(businessMap).await()
                }

                Log.d("AddNewBusinessFragment", "Business saved successfully")

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Business saved successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }

            } catch (e: Exception) {
                Log.e("AddNewBusinessFragment", "Error saving business", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to save business.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "AIzaSyDGbTBBbd13wYsrpaRzYdHg8GUDgpJ4Inc"
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string()
                Log.d("Geocoding", "API Response: $responseBody") //    Log response for debugging

                responseBody?.let {
                    val jsonResponse = JSONObject(it)
                    val results = jsonResponse.optJSONArray("results")

                    if (results != null && results.length() > 0) {
                        val location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")

                        val lat = location.getDouble("lat")
                        val lon = location.getDouble("lng")
                        Log.d("Geocoding", "Coordinates: $lat, $lon") //    Log retrieved coordinates
                        return@withContext Pair(lat, lon)
                    } else {
                        Log.e("Geocoding", "No results found for address: $address")
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
