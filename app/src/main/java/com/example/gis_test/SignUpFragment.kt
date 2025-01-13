package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.SignupPageBinding

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

            // יצירת Bundle עם נתוני המשתמש
            val userBundle = Bundle().apply {
                putString("userName", userName)
                putString("userEmail", userEmail)
                putString("userPassword", userPassword)
            }

            // ניווט לפרגמנט הבא עם הנתונים
            findNavController().navigate(
                R.id.action_signUpFragment_to_secondSignUpFragment,
                userBundle
            )
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
