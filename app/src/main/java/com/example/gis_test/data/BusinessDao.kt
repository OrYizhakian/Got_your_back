package com.example.gis_test.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BusinessDao {
    @Insert
    suspend fun insertBusiness(business: Business): Long

    @Update
    suspend fun updateBusiness(business: Business): Int

    @Query("SELECT * FROM businesses WHERE businessId = :businessId")
    suspend fun getBusinessById(businessId: Long): Business?

    @Query("SELECT * FROM businesses WHERE userId = :userId")
    suspend fun getBusinessesByUserId(userId: Long): List<Business>

    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinesses(): List<Business>

    @Query("UPDATE businesses SET latitude = :latitude, longitude = :longitude WHERE businessId = :businessId")
    suspend fun updateBusinessCoordinates(businessId: Long, latitude: Double, longitude: Double)
}