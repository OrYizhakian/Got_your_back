package com.example.gis_test.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gis_test.data.AppDatabase
import com.example.GotYourBack.R
import com.example.GotYourBack.databinding.LoginPageBinding
import kotlinx.coroutines.launch

class LoginPageFragment : Fragment() {
    private var _binding: LoginPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LoginPageBinding.inflate(inflater, container, false)

        binding.loginBtn.setOnClickListener {
            val username = binding.usernameEdt.text.toString().trim()
            val password = binding.passwordEdt.text.toString().trim()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if user exists
            lifecycleScope.launch {
                val userId = checkUserCredentials(username, password)
                if (userId != null) {
                    // User exists, navigate to MyBusinessesFragment
                    val args = Bundle().apply {
                        putLong("userId", userId)
                    }
                    findNavController().navigate(
                        R.id.action_loginPageFragment_to_myBusinessesFragment,
                        args
                    )
                } else {
                    // User does not exist, show error
                    Toast.makeText(requireContext(), "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.signupBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginPageFragment_to_signUpFragment)
        }

        return binding.root
    }

    private suspend fun checkUserCredentials(username: String, password: String): Long? {
        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        return userDao.getUserIdByCredentials(username, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
