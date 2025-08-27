package com.example.quickqr.data.repository

import com.example.quickqr.data.local.QrCodeDao
import com.example.quickqr.data.model.QrData
import com.example.quickqr.data.model.QrCodeType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QrRepository @Inject constructor(
    private val qrCodeDao: QrCodeDao
) {
    fun getAllQrCodes(): Flow<List<QrData>> = qrCodeDao.getAll()
    
    suspend fun insertQrCode(qrData: QrData) = qrCodeDao.insert(qrData)
    
    suspend fun deleteQrCode(qrData: QrData) = qrCodeDao.delete(qrData)
    
    suspend fun updateQrCode(qrData: QrData) = qrCodeDao.update(qrData)
    
    suspend fun getQrCodeById(id: String): QrData? = qrCodeDao.getById(id)
    
    fun searchQrCodes(query: String): Flow<List<QrData>> = 
        qrCodeDao.search("%$query%")
}
