package com.example.gis_test.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val userName: String,
    val email: String,
    val password: String,
    val fireBaseId:String
)
