package com.example.gis_test.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.GotYourBack.R
import com.example.gis_test.data.Business
import com.example.GotYourBack.databinding.MyBusinessScreenBinding
import com.google.android.material.snackbar.Snackbar
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

        //   Setup RecyclerView & Buttons
        setupRecyclerView()
        setupButtons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseUser = auth.currentUser
        val firebaseUserId = firebaseUser?.uid
        val email = firebaseUser?.email ?: ""

        if (firebaseUserId != null) {
            Log.d("MyBusinessesFragment", "Fetching businesses for user: $firebaseUserId")

            //   If the user is an admin, load all businesses
            if (email.endsWith("@admin.com")) {
                loadBusinesses(adminMode = true)
            } else {
                loadBusinesses(adminMode = false, userId = firebaseUserId)
            }
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
                    putDouble("latitude", business.latitude ?: 0.0)
                    putDouble("longitude", business.longitude ?: 0.0)
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
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedBusiness = adapter.currentList[position]
                val currentList = adapter.currentList.toMutableList()

                //  Remove item from UI immediately
                currentList.removeAt(position)
                adapter.submitList(currentList.toList())

                //  Show Snackbar
                val snackbar = Snackbar.make(binding.root, "Business deleted", Snackbar.LENGTH_LONG)
                snackbar.setAction("UNDO") {
                    lifecycleScope.launch {
                        try {
                            //  Restore business in Firestore
                            firestore.collection("businesses")
                                .document(deletedBusiness.businessIdFirestore)
                                .set(deletedBusiness)
                                .await()

                            //  Restore UI
                            val updatedList = adapter.currentList.toMutableList()
                            updatedList.add(position, deletedBusiness)
                            withContext(Dispatchers.Main) {
                                adapter.submitList(updatedList.toList())
                            }
                        } catch (e: Exception) {
                            Log.e("MyBusinessesFragment", "Error restoring business", e)
                            Toast.makeText(requireContext(), "Error restoring business", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                snackbar.show()

                //  If UNDO is not clicked, delete after Snackbar disappears
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) { //  Only delete if UNDO was NOT clicked
                            lifecycleScope.launch {
                                try {
                                    firestore.collection("businesses")
                                        .document(deletedBusiness.businessIdFirestore)
                                        .delete()
                                        .await()
                                } catch (e: Exception) {
                                    Log.e("MyBusinessesFragment", "Error deleting business", e)
                                }
                            }
                        }
                    }
                })
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.businessRecyclerView)

    }

    private fun setupButtons() {
        binding.addBusinessFab.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId)
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

    private fun loadBusinesses(adminMode: Boolean, userId: String? = null) {
        lifecycleScope.launch {
            try {
                val businesses = mutableListOf<Business>()

                val query = if (adminMode) {
                    //  Admin: Fetch all businesses
                    firestore.collection("businesses")
                } else {
                    // 👤 Regular User: Fetch only their businesses
                    firestore.collection("businesses").whereEqualTo("userId", userId)
                }

                val snapshot = withContext(Dispatchers.IO) { query.get().await() }

                Log.d("MyBusinessesFragment", "Firestore returned ${snapshot.documents.size} businesses.")

                snapshot.documents.mapNotNullTo(businesses) { document ->
                    val firestoreId = document.getString("businessIdFirestore") ?: document.id
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")

                    Business(
                        businessId = firestoreId.hashCode().toLong(),
                        businessIdFirestore = firestoreId,
                        userId = document.getString("userId") ?: "",
                        name = document.getString("name") ?: "",
                        category = document.getString("category") ?: "",
                        street = document.getString("street") ?: "",
                        streetNumber = document.getString("streetNumber") ?: "",
                        openingHours = document.getString("openingHours") ?: "00:00",
                        closingHours = document.getString("closingHours") ?: "00:00",
                        description = document.getString("description") ?: "",
                        latitude = lat,
                        longitude = lng
                    )
                }

                //   Update UI
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
