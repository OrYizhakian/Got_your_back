package com.example.gis_test

class UserDataFragment {
    data class User(
        val userName: String,
        val userEmail: String,
        val userPassword: String,
        val businessName: String,
        val businessCategory: String,
        val businessStreet: String,
        val businessStreetNumber: String,
        val businessOpeningHours: String,
        val businessOpeningMinutes: String,
        val businessClosingHours: String,
        val businessClosingMinutes: String,
        val businessDescription: String
    )
}