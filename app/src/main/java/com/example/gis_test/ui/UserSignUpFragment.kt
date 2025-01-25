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

class UserSignUpFragment : Fragment() {
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

            // בדיקה אם כל השדות מלאים
            if (!isInputValid(userName, userEmail, userPassword)) {
                Toast.makeText(requireContext(), "Please fill in all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // בדיקה אם האימייל תקין
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(requireContext(), "Invalid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // בדיקה אם המשתמש כבר קיים בבסיס הנתונים
            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                val existingUser = userDao.getUserByName(userName)

                if (existingUser != null) {
                    // אם המשתמש קיים כבר בבסיס הנתונים
                    Toast.makeText(requireContext(), "User already exists.", Toast.LENGTH_SHORT).show()
                } else {
                    // יצירת Bundle עם המידע של המשתמש
                    val userBundle = Bundle().apply {
                        putString("userName", userName)
                        putString("userEmail", userEmail)
                        putString("userPassword", userPassword)
                    }

                    // מעבר למסך הרשמה לעסק
                    findNavController().navigate(R.id.action_signUpFragment_to_secondSignUpFragment, userBundle)
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
