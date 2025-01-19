package com.example.gis_test.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BusinessDao {

    // Insert method now returns the inserted row ID
    @Insert
    suspend fun insertBusiness(business: Business): Long

    // Update method now returns the number of rows updated
    @Update
    suspend fun updateBusiness(business: Business): Int

    // Query to fetch a business by its ID
    @Query("SELECT * FROM businesses WHERE businessId = :businessId")
    suspend fun getBusinessById(businessId: Long): Business?

    // Query to fetch businesses by user ID
    @Query("SELECT * FROM businesses WHERE userId = :userId")
    suspend fun getBusinessesByUserId(userId: Long): List<Business>
}

