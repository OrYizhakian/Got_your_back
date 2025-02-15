package com.example.gis_test.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.GotYourBack.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.Business
import com.example.GotYourBack.databinding.MyBusinessScreenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyBusinessesFragment : Fragment() {
    private var _binding: MyBusinessScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BusinessAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyBusinessScreenBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupButtons()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = BusinessAdapter(
            onShortClick = { business ->
                val bundle = Bundle().apply {
                    putLong("businessId", business.businessId)
                }
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_businessDetailsFragment,
                    bundle
                )
            },
            onLongPress = { business ->
                val bundle = Bundle().apply {
                    putLong("focusBusinessId", business.businessId)
                    putString("firebaseUserId", auth.currentUser?.uid)
                    // Pass all business details for the map
                    putString("businessName", business.name)
                    putString("businessStreet", business.street)
                    putString("businessStreetNumber", business.streetNumber)
                    putString("businessCategory", business.category)
                }

                Log.d("MyBusinessesFragment", "Navigating to map with business: ${business.name}")
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_mapFragment2,
                    bundle
                )
            }
        )

        binding.businessRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyBusinessesFragment.adapter
        }
    }

    private fun setupButtons() {
        binding.fabAddBusiness.setOnClickListener {
            val userId = arguments?.getLong("userId") ?: -1L
            if (userId != -1L) {
                val bundle = Bundle().apply {
                    putLong("userId", userId)
                }
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_addNewBusinessFragment,
                    bundle
                )
            }
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBusinesses()
    }

    private fun loadBusinesses() {
        lifecycleScope.launch {
            try {
                val businesses = mutableListOf<Business>()

                // Get current Firebase user ID
                val firebaseUserId = auth.currentUser?.uid
                Log.d("MyBusinessesFragment", "Current Firebase User ID: $firebaseUserId")

                // Load Firebase businesses
                if (firebaseUserId != null) {
                    val firebaseBusinesses = withContext(Dispatchers.IO) {
                        val snapshot = firestore.collection("businesses")
                            .whereEqualTo("userId", firebaseUserId)
                            .get()
                            .await()

                        Log.d("MyBusinessesFragment", "Found ${snapshot.documents.size} Firebase businesses")

                        snapshot.documents.mapNotNull { document ->
                            try {
                                // Create Business object exactly matching Firebase structure
                                Business(
                                    businessId = document.id.hashCode().toLong(),
                                    userId = -1L, // We use -1 for Firebase businesses
                                    name = document.getString("name") ?: "",
                                    category = document.getString("category") ?: "",
                                    street = document.getString("street") ?: "",
                                    streetNumber = document.getString("streetNumber") ?: "",
                                    openingHours = document.getString("openingHours") ?: "00:00",
                                    closingHours = document.getString("closingHours") ?: "00:00",
                                    description = document.getString("description") ?: ""
                                ).also { business ->
                                    Log.d("MyBusinessesFragment", """
                                        Parsed Firebase business:
                                        - ID: ${business.businessId}
                                        - Name: ${business.name}
                                        - Category: ${business.category}
                                        - Address: ${business.street} ${business.streetNumber}
                                        - Hours: ${business.openingHours} - ${business.closingHours}
                                    """.trimIndent())
                                }
                            } catch (e: Exception) {
                                Log.e("MyBusinessesFragment", "Error parsing business document: ${e.message}")
                                null
                            }
                        }
                    }
                    businesses.addAll(firebaseBusinesses)
                }

                // Update UI
                withContext(Dispatchers.Main) {
                    if (businesses.isNotEmpty()) {
                        binding.businessListEmptyMessage.visibility = View.GONE
                        adapter.submitList(businesses)
                        Log.d("MyBusinessesFragment", "Displaying ${businesses.size} businesses")
                    } else {
                        binding.businessListEmptyMessage.apply {
                            text = "No businesses found."
                            visibility = View.VISIBLE
                        }
                        Log.d("MyBusinessesFragment", "No businesses found to display")
                    }
                }

            } catch (e: Exception) {
                Log.e("MyBusinessesFragment", "Error loading businesses", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading businesses: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.businessListEmptyMessage.apply {
                        text = "Error loading businesses"
                        visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}