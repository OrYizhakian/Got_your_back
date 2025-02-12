package com.example.gis_test.ui

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
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
import com.example.gis_test.data.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val password = binding.passwordEdt.text.toString().trim()
        val userEmail = binding.emailEdt.text.toString().trim()

        if (!validateInput(userEmail, password)) {
            return
        }

        lifecycleScope.launch {
            try {
                // First try local authentication
                val localUserId = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext())
                        .userDao()
                        .getUserIdByCredentials(userEmail, password)
                }

                if (localUserId != null) {
                    navigateToBusinesses(localUserId)
                    return@launch
                }

                // If local auth fails, try Firebase
                authenticateWithFirebase(userEmail, password)
            } catch (e: Exception) {
                Log.e(TAG, "Error during login", e)
                showError("Login failed: ${e.message}")
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            showError("Please fill in all fields")
            return false
        }
        return true
    }

    private fun authenticateWithFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleSuccessfulFirebaseAuth(email, password)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    showError("Authentication failed")
                }
            }
    }

    private fun handleSuccessfulFirebaseAuth(email: String, password: String) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            showError("Failed to get user details")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userDao = AppDatabase.getDatabase(requireContext()).userDao()

                // Check if user exists in Room
                var localUser = userDao.getUserByName(email)

                if (localUser == null) {
                    // Create new user in Room
                    val userId = userDao.insertUser(
                        User(
                            userName = email,
                            email = email,
                            password = password,
                            fireBaseId = firebaseUser.uid
                        )
                    )
                    localUser = userDao.getUserById(userId)
                }

                withContext(Dispatchers.Main) {
                    if (localUser != null) {
                        navigateToBusinesses(localUser.userId)
                    } else {
                        showError("Failed to create local user")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating local user", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to create local user: ${e.message}")
                }
            }
        }
    }

    private fun navigateToBusinesses(userId: Long) {
        val args = Bundle().apply {
            putLong("userId", userId)
        }
        findNavController().navigate(
            R.id.action_loginPageFragment_to_myBusinessesFragment,
            args
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}