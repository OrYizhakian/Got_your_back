package com.example.gis_test.data

import androidx.room.*

@Dao
interface BusinessDao {
    @Insert
    suspend fun insertBusiness(business: Business): Long

    @Query("SELECT * FROM businesses WHERE userId = :userId")
    suspend fun getBusinessesByUserId(userId: Long): List<Business>

    @Delete
    suspend fun deleteBusiness(business: Business)
}
