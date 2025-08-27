package com.example.quickqr.util

import android.net.Uri
import com.example.quickqr.data.model.QrCodeType
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeProcessor @Inject constructor() {

    suspend fun processBarcode(barcode: Barcode): QrCodeResult {
        return withContext(Dispatchers.IO) {
            try {
                val content = when (barcode.valueType) {
                    Barcode.TYPE_URL -> {
                        val url = barcode.url?.url?.toString() ?: ""
                        QrCodeResult.Success(url, QrCodeType.URL)
                    }
                    Barcode.TYPE_WIFI -> {
                        val ssid = barcode.wifi?.ssid ?: ""
                        val password = barcode.wifi?.password ?: ""
                        val type = barcode.wifi?.encryptionType ?: 0
                        "WIFI:S:$ssid;T:${getWifiType(type)};P:$password;;"
                            .let { QrCodeResult.Success(it, QrCodeType.WIFI) }
                    }
                    Barcode.TYPE_CONTACT_INFO -> {
                        val contact = barcode.contactInfo
                        val contactString = buildString {
                            contact?.name?.formattedName?.let { append("Name: $it\n") }
                            contact?.organization?.let { append("Org: $it\n") }
                            contact?.emails?.firstOrNull()?.address?.let { append("Email: $it\n") }
                            contact?.phones?.firstOrNull()?.number?.let { append("Phone: $it\n") }
                        }
                        QrCodeResult.Success(contactString, QrCodeType.TEXT)
                    }
                    Barcode.TYPE_EMAIL -> {
                        val email = barcode.email?.address ?: ""
                        val subject = barcode.email?.subject ?: ""
                        val body = barcode.email?.body ?: ""
                        "mailto:$email?subject=$subject&body=$body"
                            .let { QrCodeResult.Success(it, QrCodeType.URL) }
                    }
                    Barcode.TYPE_PHONE -> {
                        val phone = barcode.phone?.number ?: ""
                        "tel:$phone".let { QrCodeResult.Success(it, QrCodeType.URL) }
                    }
                    Barcode.TYPE_SMS -> {
                        val number = barcode.sms?.phoneNumber ?: ""
                        val message = barcode.sms?.message ?: ""
                        "smsto:$number:$message".let { QrCodeResult.Success(it, QrCodeType.URL) }
                    }
                    Barcode.TYPE_GEO -> {
                        val lat = barcode.geoPoint?.lat ?: 0.0
                        val lng = barcode.geoPoint?.lng ?: 0.0
                        "geo:$lat,$lng".let { QrCodeResult.Success(it, QrCodeType.URL) }
                    }
                    Barcode.TYPE_CALENDAR_EVENT -> {
                        val event = barcode.calendarEvent
                        val eventString = buildString {
                            event?.summary?.let { append("Event: $it\n") }
                            event?.description?.let { append("$it\n") }
                            event?.location?.let { append("Location: $it\n") }
                            event?.start?.let { append("Start: ${it.year}/${it.month}/${it.day} ${it.hours}:${it.minutes}\n") }
                            event?.end?.let { append("End: ${it.year}/${it.month}/${it.day} ${it.hours}:${it.minutes}\n") }
                        }
                        QrCodeResult.Success(eventString, QrCodeType.TEXT)
                    }
                    Barcode.TYPE_DRIVER_LICENSE -> {
                        val license = barcode.driverLicense
                        val licenseString = buildString {
                            license?.licenseNumber?.let { append("License #: $it\n") }
                            license?.firstName?.let { append("First: $it\n") }
                            license?.middleName?.let { append("Middle: $it\n") }
                            license?.lastName?.let { append("Last: $it\n") }
                            license?.gender?.let { append("Gender: $it\n") }
                            license?.birthDate?.let { append("DOB: $it\n") }
                            license?.addressCity?.let { append("City: $it\n") }
                            license?.addressState?.let { append("State: $it\n") }
                            license?.addressZip?.let { append("ZIP: $it\n") }
                            license?.issueDate?.let { append("Issued: $it\n") }
                            license?.expiryDate?.let { append("Expires: $it\n") }
                        }
                        QrCodeResult.Success(licenseString, QrCodeType.TEXT)
                    }
                    else -> {
                        QrCodeResult.Success(barcode.rawValue ?: "", QrCodeType.TEXT)
                    }
                }
                content
            } catch (e: Exception) {
                QrCodeResult.Error(e.message ?: "Unknown error processing QR code")
            }
        }
    }

    private fun getWifiType(type: Int): String = when (type) {
        Barcode.WiFi.TYPE_OPEN -> "nopass"
        Barcode.WiFi.TYPE_WEP -> "WEP"
        Barcode.WiFi.TYPE_WPA -> "WPA"
        Barcode.WiFi.TYPE_WPA2 -> "WPA2"
        Barcode.WiFi.TYPE_WPA3 -> "WPA3"
        else -> "nopass"
    }

    suspend fun processImageUri(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            // Implementation for processing image from URI
            // This would use ML Kit to process the image and extract QR code
            // For now, returning a placeholder
            ""
        }
    }

    suspend fun isValidUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class QrCodeResult {
    data class Success(val content: String, val type: QrCodeType) : QrCodeResult()
    data class Error(val message: String) : QrCodeResult()
}
