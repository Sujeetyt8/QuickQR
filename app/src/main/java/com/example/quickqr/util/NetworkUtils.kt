package com.example.quickqr.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Utility class for network-related operations.
 */
object NetworkUtils {

    /**
     * Check if the device is currently connected to the internet.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    /**
     * Check if the device is connected to a metered network.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun isMeteredNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.isActiveNetworkMetered
        }
    }

    /**
     * Get the type of network the device is currently connected to.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                ConnectivityManager.TYPE_VPN -> NetworkType.VPN
                else -> NetworkType.OTHER
            }
        }
    }

    /**
     * Observe network connectivity changes as a LiveData.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun observeNetworkConnectivity(context: Context): LiveData<Boolean> {
        return NetworkConnectionLiveData(context)
    }

    /**
     * Observe network connectivity changes as a Flow.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun observeNetworkConnectivityFlow(context: Context): Flow<Boolean> {
        return callbackFlow {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(false)
                }
            }

            // Initial state
            trySend(isNetworkAvailable(context))

            // Register for updates
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(request, networkCallback)

            // Unregister when no longer needed
            awaitClose {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }.distinctUntilChanged()
    }

    /**
     * Network type enumeration.
     */
    enum class NetworkType {
        WIFI,
        MOBILE,
        ETHERNET,
        VPN,
        OTHER,
        NONE
    }

    /**
     * LiveData that tracks network connectivity status.
     */
    private class NetworkConnectionLiveData(
        private val context: Context
    ) : LiveData<Boolean>() {
        private val connectivityManager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                postValue(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                postValue(false)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                postValue(false)
            }
        }

        override fun onActive() {
            super.onActive()
            updateConnectionStatus()
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

        private fun updateConnectionStatus() {
            postValue(isNetworkAvailable(context))
        }
    }

    /**
     * Check if the device is connected to a high-speed network.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun isHighSpeedNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING))
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    /**
     * Get the current network's download speed in bits per second.
     * Returns -1 if the speed cannot be determined.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetworkDownloadSpeedBps(context: Context): Long {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return -1
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return -1
        
        return capabilities.linkDownstreamBandwidthKbps * 1000L // Convert kbps to bps
    }

    /**
     * Get the current network's upload speed in bits per second.
     * Returns -1 if the speed cannot be determined.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetworkUploadSpeedBps(context: Context): Long {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return -1
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return -1
        
        return capabilities.linkUpstreamBandwidthKbps * 1000L // Convert kbps to bps
    }
}
