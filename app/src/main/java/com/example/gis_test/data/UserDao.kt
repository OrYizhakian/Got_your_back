package com.example.gis_test.data

import androidx.room.*

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE userId = :id")
    suspend fun getUserById(id: Long): User?

    @Delete
    suspend fun deleteUser(user: User)
}
