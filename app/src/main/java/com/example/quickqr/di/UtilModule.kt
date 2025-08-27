package com.example.quickqr.di

import android.content.Context
import com.example.quickqr.util.CameraManager
import com.example.quickqr.util.ImageUtils
import com.example.quickqr.util.NetworkManager
import com.example.quickqr.util.PermissionManager
import com.example.quickqr.util.QrCodeProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideImageUtils(@ApplicationContext context: Context): ImageUtils {
        return ImageUtils(context)
    }

    @Provides
    @Singleton
    fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager {
        return NetworkManager(context)
    }

    @Provides
    @Singleton
    fun provideQrCodeProcessor(): QrCodeProcessor {
        return QrCodeProcessor()
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context, {}, {})
    }

    @Provides
    fun provideCameraManager(
        @ApplicationContext context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        onBarcodeDetected: (String) -> Unit
    ): CameraManager {
        return CameraManager(context, lifecycleOwner, onBarcodeDetected)
    }
}
