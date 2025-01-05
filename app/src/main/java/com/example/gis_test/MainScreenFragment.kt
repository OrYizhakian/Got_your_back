package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.MainScreenPageBinding

class MainScreenFragment: Fragment() {
    private var _binding: MainScreenPageBinding?= null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MainScreenPageBinding.inflate(inflater,container,false)
        binding.singingButton.setOnClickListener{
            findNavController().navigate(R.id.action_mainScreenFragment_to_loginPageFragment)
        }
        binding.mapButton.setOnClickListener{
            findNavController().navigate(R.id.action_mainScreenFragment_to_mapFragment2)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}