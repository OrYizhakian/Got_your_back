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
import com.example.GotYourBack.databinding.SignupPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSignUpFragment : Fragment() {
    private var _binding: SignupPageBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SignupPageBinding.inflate(inflater, container, false)

        binding.continueBtn.setOnClickListener {
            val userName = binding.usernameEdt.text.toString().trim()
            val userEmail = binding.emailEdt.text.toString().trim()
            val userPassword = binding.passwordEdt.text.toString().trim()

            if (!isInputValid(userName, userEmail, userPassword)) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(requireContext(), "Invalid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkUserExists(userName, userEmail, userPassword)
        }

        return binding.root
    }

    private fun isInputValid(vararg inputs: String): Boolean {
        return inputs.all { it.isNotBlank() }
    }

    private fun checkUserExists(userName: String, userEmail: String, userPassword: String) {
        if (userEmail.endsWith("@admin.com")) {
            Toast.makeText(requireContext(), "Admin emails are not allowed for sign-up.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                // 🔍 Check Firestore `users` collection for existing email
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    // ❌ User already exists
                    Toast.makeText(requireContext(), "User already exists. Please log in.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ User does not exist → Proceed to Business Signup
                val userBundle = Bundle().apply {
                    putString("userName", userName)
                    putString("userEmail", userEmail)
                    putString("userPassword", userPassword)
                }

                findNavController().navigate(R.id.action_signUpFragment_to_secondSignUpFragment, userBundle)

            } catch (e: Exception) {
                Log.e("UserCheck", "Error checking Firestore for existing user", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
