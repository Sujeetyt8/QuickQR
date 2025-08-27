package com.example.quickqr.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class PermissionManager(
    private val context: Context,
    private val onPermissionsGranted: () -> Unit = {},
    private val onPermissionsDenied: (List<String>) -> Unit = {}
) {
    private var requiredPermissions = mutableListOf<String>()
    private var permissionRequest: (() -> Unit)? = null

    companion object {
        // Common permission groups
        val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
        val STORAGE_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun withPermissions(vararg permissions: String): PermissionManager {
        requiredPermissions.addAll(permissions)
        return this
    }

    fun withPermissions(permissions: List<String>): PermissionManager {
        requiredPermissions.addAll(permissions)
        return this
    }

    fun checkPermissions(activity: Activity) {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            onPermissionsGranted()
        } else {
            val launcher = (activity as? FragmentActivity)?.activityResultRegistry?.register(
                "permission_request",
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val deniedPermissions = permissions.filter { !it.value }.map { it.key }
                if (deniedPermissions.isEmpty()) {
                    onPermissionsGranted()
                } else {
                    onPermissionsDenied(deniedPermissions)
                }
            }
            launcher?.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun checkPermissions(fragment: Fragment) {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            onPermissionsGranted()
        } else {
            val launcher = fragment.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val deniedPermissions = permissions.filter { !it.value }.map { it.key }
                if (deniedPermissions.isEmpty()) {
                    onPermissionsGranted()
                } else {
                    onPermissionsDenied(deniedPermissions)
                }
            }
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    fun rememberPermissionState(permission: String) = rememberPermissionState(permission) { isGranted ->
        if (isGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied(listOf(permission))
        }
    }

    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return (activity as? FragmentActivity)?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    fun shouldShowRequestPermissionRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    fun openAppSettings(activity: Activity) {
        // Implementation to open app settings
        // This would typically use an Intent to open the app's settings page
    }

    fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getFirstDeniedPermission(): String? {
        return requiredPermissions.firstOrNull {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun getGrantedPermissions(): List<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getDeniedPermissions(): List<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun clear() {
        requiredPermissions.clear()
        permissionRequest = null
    }
}
