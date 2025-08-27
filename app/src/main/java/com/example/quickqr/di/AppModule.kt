package com.example.quickqr.di

import android.content.Context
import com.example.quickqr.data.local.AppDatabase
import com.example.quickqr.data.local.QrCodeDao
import com.example.quickqr.data.repository.QrRepository
import com.example.quickqr.data.repository.QrRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideQrCodeDao(database: AppDatabase): QrCodeDao {
        return database.qrCodeDao()
    }

    @Provides
    fun provideQrRepository(qrCodeDao: QrCodeDao): QrRepository {
        return QrRepositoryImpl(qrCodeDao)
    }
}
