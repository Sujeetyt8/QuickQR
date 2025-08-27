package com.example.quickqr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.quickqr.Fragment.QrCodeType
import java.util.*

@Entity(tableName = "qr_codes")
data class QrData(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: QrCodeType,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
