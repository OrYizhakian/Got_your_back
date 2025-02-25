package com.example.gis_test.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize // ✅ This makes Business Parcelable
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
    val businessIdFirestore: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val openingHours: String = "",
    val closingHours: String = "",
    val description: String? = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable // ✅ Implements Parcelable
