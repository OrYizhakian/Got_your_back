import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.databinding.MyBusinessScreenBinding
import kotlinx.coroutines.launch

class MyBusinessesFragment : Fragment() {
    private var _binding: MyBusinessScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyBusinessScreenBinding.inflate(inflater, container, false)

        // Set up RecyclerView
        val adapter = BusinessAdapter(onShortClick = { business ->
            // Navigate to BusinessDetailsFragment
            val bundle = Bundle().apply {
                putLong("businessId", business.businessId)
            }
            findNavController().navigate(R.id.action_myBusinessesFragment_to_businessDetailsFragment, bundle)
        },
            onLongPress = { business ->
                // Navigate to MapFragment with the business's address
                val bundle = Bundle().apply {
                    putString("street", "${business.street} ${business.streetNumber}")
                }
                findNavController().navigate(R.id.action_myBusinessesFragment_to_mapFragment2, bundle)
            }
        )
        binding.businessRecyclerView.adapter = adapter



        binding.businessRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.businessRecyclerView.adapter = adapter

        // Get userId from arguments
        val userId = arguments?.getLong("userId") ?: -1L
        if (userId == -1L) {
            binding.businessListEmptyMessage.text = "Error: User ID is missing."
        } else {
            // Fetch businesses from the database
            lifecycleScope.launch {
                val businesses = AppDatabase.getDatabase(requireContext()).businessDao().getBusinessesByUserId(userId)
                if (businesses.isEmpty()) {
                    binding.businessListEmptyMessage.text = "No businesses found for this user."
                    binding.businessListEmptyMessage.visibility = View.VISIBLE
                } else {
                    binding.businessListEmptyMessage.visibility = View.GONE
                    adapter.submitList(businesses)
                }
            }
        }

        binding.fabAddBusiness.setOnClickListener {
            val bundle = Bundle()
            bundle.putLong("userId", userId)
            findNavController().navigate(R.id.action_myBusinessesFragment_to_secondSignUpFragment, bundle)
        }


        // Handle back button click
        binding.mrsBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
