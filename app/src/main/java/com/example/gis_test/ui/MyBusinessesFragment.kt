import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.databinding.MyBusinessScreenBinding
import kotlinx.coroutines.launch

class MyBusinessesFragment : Fragment() {
    private var _binding: MyBusinessScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BusinessAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyBusinessScreenBinding.inflate(inflater, container, false)

        // Set up RecyclerView adapter
        adapter = BusinessAdapter(
            onShortClick = { business ->
                // Navigate to BusinessDetailsFragment
                val bundle = Bundle().apply {
                    putLong("businessId", business.businessId)
                }
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_businessDetailsFragment,
                    bundle
                )
            },
            onLongPress = { business ->
                // Navigate to MapFragment with the business's address
                val bundle = Bundle().apply {
                    putString("street", "${business.street} ${business.streetNumber}")
                }
                findNavController().navigate(
                    R.id.action_myBusinessesFragment_to_mapFragment2,
                    bundle
                )
            }
        )

        // Set up RecyclerView
        binding.businessRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.businessRecyclerView.adapter = adapter

        // Floating action button to add a new business
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
            } else {
                Log.e("MyBusinessesFragment", "User ID missing, cannot navigate to AddNewBusinessFragment")
            }
        }

        // Back button functionality
        binding.mrsBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch businesses based on userId from arguments
        val userId = arguments?.getLong("userId") ?: -1L
        if (userId == -1L) {
            Log.e("MyBusinessesFragment", "User ID missing")
            binding.businessListEmptyMessage.text = "Error: User ID is missing."
            binding.businessListEmptyMessage.visibility = View.VISIBLE
            return
        }

        // Load data from database
        lifecycleScope.launch {
            val businesses = AppDatabase.getDatabase(requireContext())
                .businessDao()
                .getBusinessesByUserId(userId)

            if (businesses.isNotEmpty()) {
                binding.businessListEmptyMessage.visibility = View.GONE
                adapter.submitList(businesses)
            } else {
                binding.businessListEmptyMessage.text = "No businesses found for this user."
                binding.businessListEmptyMessage.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
