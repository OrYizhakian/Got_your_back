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
import com.example.GotYourBack.R
import com.example.GotYourBack.databinding.LoginPageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginPageFragment : Fragment() {
    private var _binding: LoginPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginPageBinding.inflate(inflater, container, false)
        setupListeners()
        return binding.root
    }

    private fun setupListeners() {
        binding.loginBtn.setOnClickListener {
            handleLogin()
        }

        binding.signupBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginPageFragment_to_signUpFragment)
        }
    }

    private fun handleLogin() {
        val email = binding.emailEdt.text.toString().trim()
        val password = binding.passwordEdt.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                    val firebaseUserId =
                        authResult.user?.uid ?: throw Exception("Failed to retrieve user ID")

                    Log.d("LoginFragment", "User logged in with ID: $firebaseUserId")

                    // ✅ Navigate to MyBusinessesFragment and pass the user ID
                    val bundle = Bundle().apply {
                        putString("userId", firebaseUserId)
                    }
                    findNavController().navigate(
                        R.id.action_loginPageFragment_to_myBusinessesFragment, bundle
                    )

                } catch (e: Exception) {
                    Log.e("LoginFragment", "Login failed", e)
                    Toast.makeText(
                        requireContext(),
                        "Login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
