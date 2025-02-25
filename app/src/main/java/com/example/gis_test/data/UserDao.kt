package com.example.gis_test.data

import androidx.room.*

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE userId = :id")
    suspend fun getUserById(id: String): User?

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT userId FROM users WHERE userName = :username AND password = :password")
    suspend fun getUserIdByCredentials(username: String, password: String): Long?

    @Query("SELECT * FROM users WHERE userName = :username")
    suspend fun getUserByName(username: String): User?
}

