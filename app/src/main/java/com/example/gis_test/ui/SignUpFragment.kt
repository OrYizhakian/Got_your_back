package com.example.gis_test.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gis_test.R
import com.example.gis_test.data.AppDatabase
import com.example.gis_test.data.User
import com.example.gis_test.databinding.SignupPageBinding
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {
    private var _binding: SignupPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SignupPageBinding.inflate(inflater, container, false)

        binding.continueBtn.setOnClickListener {
            val userName = binding.usernameEdt.text.toString().trim()
            val userEmail = binding.emailEdt.text.toString().trim()
            val userPassword = binding.passwordEdt.text.toString().trim()

            if (!isInputValid(userName, userEmail, userPassword)) {
                Toast.makeText(requireContext(), "יש למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(requireContext(), "כתובת האימייל שגויה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = User(name = userName, email = userEmail, password = userPassword)

            // Save the user in the database
            lifecycleScope.launch {
                try {
                    val userId = AppDatabase.getDatabase(requireContext()).userDao().insertUser(user)
                    val userBundle = Bundle().apply {
                        putLong("userId", userId)
                    }
                    findNavController().navigate(
                        R.id.action_signUpFragment_to_secondSignUpFragment,
                        userBundle
                    )
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to save user. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return binding.root
    }

    private fun isInputValid(vararg inputs: String): Boolean {
        return inputs.all { it.isNotBlank() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
