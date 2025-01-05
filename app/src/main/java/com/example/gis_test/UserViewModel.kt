package com.example.gis_test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

public class UserViewModel: ViewModel() {
    public val user = MutableLiveData<UserDataFragment.User>()
    val latitude: MutableLiveData<Double> = MutableLiveData()
    val longitude: MutableLiveData<Double> = MutableLiveData()
}