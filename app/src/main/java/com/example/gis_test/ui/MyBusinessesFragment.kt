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

        // ✅ Setup RecyclerView & Buttons
        setupRecyclerView()
        setupButtons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseUserId = arguments?.getString("userId") ?: auth.currentUser?.uid

        if (firebaseUserId != null) {
            Log.d("MyBusinessesFragment", "Fetching businesses for user: $firebaseUserId")
            loadBusinesses(firebaseUserId)
        } else {
            Log.e("MyBusinessesFragment", "No user ID found")
            Toast.makeText(requireContext(), "Error: No user ID found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = BusinessAdapter(
            onShortClick = { business ->
                val bundle = Bundle().apply {
                    putParcelable("business", business)
                }
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_businessDetailsFragment2,
                    bundle
                )
            },
            onLongPress = { business ->
                val bundle = Bundle().apply {
                    putParcelable("business", business)
                }
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
            val userId = arguments?.getString("userId") ?: auth.currentUser?.uid
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId) // ✅ Fix: Use putString() instead of putLong()
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

    private fun loadBusinesses(userId: String) {
        lifecycleScope.launch {
            try {
                val businesses = mutableListOf<Business>()

                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("businesses")
                        .whereEqualTo("userId", userId)  // ✅ Fetch only businesses for logged-in user
                        .get()
                        .await()
                }

                Log.d("MyBusinessesFragment", "Firestore returned ${snapshot.documents.size} businesses.")

                snapshot.documents.mapNotNullTo(businesses) { document ->
                    val firestoreId = document.getString("businessIdFirestore") ?: document.id
                    Log.d("MyBusinessesFragment", "Retrieved Firestore ID: $firestoreId")

                    Business(
                        businessId = firestoreId.hashCode().toLong(), // ✅ Use Firestore ID correctly
                        businessIdFirestore = firestoreId,
                        userId = document.getString("userId") ?: "",
                        name = document.getString("name") ?: "",
                        category = document.getString("category") ?: "",
                        street = document.getString("street") ?: "",
                        streetNumber = document.getString("streetNumber") ?: "",
                        openingHours = document.getString("openingHours") ?: "00:00",
                        closingHours = document.getString("closingHours") ?: "00:00",
                        description = document.getString("description") ?: ""
                    )
                }

                // ✅ Update UI
                withContext(Dispatchers.Main) {
                    if (businesses.isNotEmpty()) {
                        binding.businessListEmptyMessage.visibility = View.GONE
                        adapter.submitList(businesses)
                    } else {
                        binding.businessListEmptyMessage.apply {
                            text = "No businesses found."
                            visibility = View.VISIBLE
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("MyBusinessesFragment", "Error loading businesses", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading businesses", Toast.LENGTH_LONG).show()
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
