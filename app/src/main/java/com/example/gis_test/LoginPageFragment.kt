package com.example.gis_test

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.LoginPageBinding

class LoginPageFragment: Fragment() {
    private var _binding: LoginPageBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LoginPageBinding.inflate(inflater,container,false)
        binding.loginBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginPageFragment_to_mapFragment2)
        }
        binding.signupBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginPageFragment_to_signUpFragment)
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