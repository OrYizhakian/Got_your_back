package com.example.gis_test.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BusinessDao {

    @Insert
    suspend fun insertBusiness(business: Business)

    @Update
    suspend fun updateBusiness(business: Business)

    @Query("SELECT * FROM businesses WHERE businessId = :businessId")
    suspend fun getBusinessById(businessId: Long): Business?

    // הוסף את השיטה הזאת
    @Query("SELECT * FROM businesses WHERE userId = :userId")
    suspend fun getBusinessesByUserId(userId: Long): List<Business>
}
