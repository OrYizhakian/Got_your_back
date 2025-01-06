package com.example.gis_test

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel: ViewModel() {
    val user = MutableLiveData<UserDataFragment.User>()
    val latitude: MutableLiveData<Double> = MutableLiveData()
    val longitude: MutableLiveData<Double> = MutableLiveData()


    fun saveUserToFirestore(user: UserDataFragment.User, latitude: Double?, longitude: Double?) {
        val db = FirebaseFirestore.getInstance()

        // הכנת הנתונים לשמירה
        val userData = hashMapOf(
            "userName" to user.userName,
            "userEmail" to user.userEmail,
            "userPassword" to user.userPassword,
            "businessName" to user.businessName,
            "businessCategory" to user.businessCategory,
            "businessStreet" to user.businessStreet,
            "businessStreetNumber" to user.businessStreetNumber,
            "businessOpeningHours" to user.businessOpeningHours,
            "businessOpeningMinutes" to user.businessOpeningMinutes,
            "businessClosingHours" to user.businessClosingHours,
            "businessClosingMinutes" to user.businessClosingMinutes,
            "businessDescription" to user.businessDescription,
            "latitude" to latitude,
            "longitude" to longitude
        )

        // שמירת הנתונים למסמך
        db.collection("users")
            .add(userData)
            .addOnSuccessListener { documentReference ->
                println("User added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding user: $e")
            }
    }
    fun updateUser(user: UserDataFragment.User) {
        // אנחנו מבצעים עדכון של הנתונים בתוך ה-UserLiveData
        this.user.value = user
    }
}