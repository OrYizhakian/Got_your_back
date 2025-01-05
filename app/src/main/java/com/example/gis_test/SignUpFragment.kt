package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.SignupPageBinding


private lateinit var userViewModel: UserViewModel

class SignUpFragment: Fragment() {
    private var _binding: SignupPageBinding? = null
    private val binding get()= _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SignupPageBinding.inflate(inflater,container,false)
        userViewModel= ViewModelProvider(this)[UserViewModel::class.java]
        binding.continueBtn.setOnClickListener {

            val userName = binding.usernameEdt.text.toString()
            val userEmail = binding.emailEdt.text.toString()
            val userPassword = binding.passwordEdt.text.toString()

                /*if(userName.isBlank()||userEmail.isBlank()||userPassword.isBlank()){
                return@setOnClickListener
            }*/

            val user= UserDataFragment.User(
                userName = userName,
                userEmail = userEmail,
                userPassword = userPassword,
                businessName ="",
                businessCategory = "",
                businessStreet = "",
                businessStreetNumber = "",
                businessOpeningHours = "",
                businessClosingHours = "",
                businessOpeningMinutes = "",
                businessClosingMinutes = "",
                businessDescription = ""
        )
            userViewModel.user.value=user
            findNavController().navigate(R.id.action_signUpFragment_to_secondSignUpFragment)

        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}