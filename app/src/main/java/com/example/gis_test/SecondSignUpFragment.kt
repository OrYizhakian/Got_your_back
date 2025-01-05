package com.example.gis_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.gis_test.databinding.SignupSecPageBinding



class SecondSignUpFragment : Fragment() {
    private var _binding: SignupSecPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels() // אתחול ViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SignupSecPageBinding.inflate(inflater, container, false)
        val categories = arrayOf(
            "מסעדה",
            "בית קפה",
            "סלון יופי",
            "מכולת",
            "חנות בגדים",
            "חנות ספרים",
            "מכון כושר",
            "בית מרקחת",
            "חנות חומרי בניין",
            "חנות תכשיטים"
        )
        val hours = arrayOf(
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "20", "21", "22", "23"
        )
        val minutes = arrayOf(
            "00", "15", "30", "45"
        )

        binding.openHourPicker.minValue=0
        binding.openHourPicker.maxValue = hours.size-1
        binding.openMinutePicker.minValue=0
        binding.openMinutePicker.maxValue = minutes.size-1
        binding.closeHourPicker.minValue=0
        binding.closeHourPicker.maxValue = hours.size-1
        binding.closeMinutePicker.minValue=0
        binding.closeMinutePicker.maxValue = minutes.size-1
        binding.openHourPicker.displayedValues = hours
        binding.openMinutePicker.displayedValues = minutes
        binding.closeHourPicker.displayedValues = hours
        binding.closeMinutePicker.displayedValues = minutes
        binding.categoryPicker.minValue = 0
        binding.categoryPicker.maxValue = (categories.size-1)
        binding.categoryPicker.displayedValues = categories

        binding.signupBtn.setOnClickListener {
            val user = viewModel.user.value

            // בדיקה אם user קיים
//            if (user == null) {
//                println("Error: User is null")
//                return@setOnClickListener
//            }

            val businessName = binding.businessNameEdt.text.toString()
            val businessCategory = binding.categoryPicker.toString()
            val businessStreet = binding.streetNameEdt.text.toString()
            val businessStreetNumber = binding.streetnumberEdt.text.toString()
            val businessOpeningHours = binding.openHourPicker.toString()
            val businessOpeningMinutes = binding.openMinutePicker.toString()
            val businessClosingHours = binding.closeHourPicker.toString()
            val businessClosingMinutes = binding.closeMinutePicker.toString()
            val businessDescription = binding.businessDescEdt.text.toString()

            // בדיקת ערכים ריקים
//            if (businessName.isBlank() || businessCategory.isBlank() || businessStreet.isBlank()
//                || businessStreetNumber.isBlank()
//            ) {
//                println("Error: One or more fields are blank")
//                return@setOnClickListener
//            }

            // עדכון הנתונים ב-ViewModel
            val updatedUser = user?.copy(
                businessName = businessName,
                businessCategory = businessCategory,
                businessStreet = businessStreet,
                businessStreetNumber = businessStreetNumber,
                businessOpeningHours = businessOpeningHours,
                businessOpeningMinutes = businessOpeningMinutes,
                businessClosingHours = businessClosingHours,
                businessClosingMinutes =  businessClosingMinutes,
                businessDescription = businessDescription
            )

            viewModel.user.value = updatedUser

            // ניווט לפרגמנט הבא
            findNavController().navigate(R.id.action_secondSignUpFragment_to_loginPageFragment)
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
