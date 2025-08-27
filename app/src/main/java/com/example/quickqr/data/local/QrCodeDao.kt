package com.example.quickqr.data.local

import androidx.room.*
import com.example.quickqr.data.model.QrData
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {
    @Query("SELECT * FROM qr_codes ORDER BY timestamp DESC")
    fun getAll(): Flow<List<QrData>>
    
    @Query("SELECT * FROM qr_codes WHERE id = :id")
    suspend fun getById(id: String): QrData?
    
    @Query("SELECT * FROM qr_codes WHERE content LIKE :query OR type LIKE :query ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<QrData>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qrData: QrData)
    
    @Update
    suspend fun update(qrData: QrData)
    
    @Delete
    suspend fun delete(qrData: QrData)
    
    @Query("DELETE FROM qr_codes")
    suspend fun deleteAll()
}
