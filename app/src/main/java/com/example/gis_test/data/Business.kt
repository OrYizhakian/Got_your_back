package com.example.gis_test.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "businesses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["userId"])]
)
data class Business(
    @PrimaryKey(autoGenerate = true) val businessId: Long = 0,
    val userId: Long,
    val name: String,
    val category: String,
    val street: String,
    val streetNumber: String,
    val openingHours: String,
    val closingHours: String,
    val description: String?
)
