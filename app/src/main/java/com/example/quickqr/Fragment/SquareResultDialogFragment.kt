package com.example.quickqr.Fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.quickqr.R
import com.example.quickqr.databinding.DialogSquareResultBinding

class SquareResultDialogFragment : DialogFragment() {

    // Interface to communicate back to the HomeFragment when the dialog is dismissed
    interface OnDialogDismissListener {
        fun onDialogDismissed()
    }

    private var dismissListener: OnDialogDismissListener? = null
    private var _binding: DialogSquareResultBinding? = null
    private val binding get() = _binding!!

    fun setOnDismissListener(listener: OnDialogDismissListener) {
        this.dismissListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSquareResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scannedResult = arguments?.getString(ARG_SCANNED_RESULT) ?: "No result"
        val qrCodeType = arguments?.getSerializable(ARG_QR_CODE_TYPE) as? QrCodeType ?: QrCodeType.TEXT

        binding.qrResultText.text = scannedResult

        // Configure the dialog's UI based on the QR code type
        when (qrCodeType) {
            QrCodeType.URL -> setupForUrl(scannedResult)
            QrCodeType.WIFI -> setupForWifi(scannedResult)
            QrCodeType.TEXT -> setupForText(scannedResult)
        }

        // Set up click listeners for all buttons with improved error handling
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.copyButton.setOnClickListener { copyToClipboard(scannedResult) }
        binding.shareButton.setOnClickListener { shareResult(scannedResult) }
        binding.openLinkButton.setOnClickListener { openUrl(scannedResult) }
        binding.connectWifiButton.setOnClickListener { connectToWifi(scannedResult) }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Notify the listener (HomeFragment) that the dialog has been dismissed
        dismissListener?.onDialogDismissed()
    }

    override fun onStart() {
        super.onStart()
        // Apply a transparent background to show the rounded corners of the CardView
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- UI Setup Functions ---
    private fun setupForUrl(url: String) {
        binding.dialogTitle.text = "Website Link"
        binding.dialogIcon.setImageResource(R.drawable.ic_open_link)
        binding.openLinkButton.visibility = View.VISIBLE
        binding.copyButton.visibility = View.VISIBLE
        binding.shareButton.visibility = View.VISIBLE

        // Show domain for better user understanding
        try {
            val uri = Uri.parse(url)
            val domain = uri.host ?: url
            if (domain != url && domain.isNotEmpty()) {
                binding.qrResultText.text = domain
            }
        } catch (e: Exception) {
            // Keep original URL if parsing fails
            Log.w("SquareResultDialog", "Failed to parse URL domain", e)
        }
    }

    private fun setupForWifi(wifiData: String) {
        binding.dialogTitle.text = "Wi-Fi Network"
        binding.dialogIcon.setImageResource(R.drawable.ic_wifi)
        binding.connectWifiButton.visibility = View.VISIBLE
        binding.copyButton.visibility = View.VISIBLE

        // Parse and show Wi-Fi network name for better UX
        try {
            val ssid = parseWifiSsid(wifiData)
            if (ssid.isNotEmpty()) {
                binding.qrResultText.text = "Network: $ssid"
            }
        } catch (e: Exception) {
            Log.w("SquareResultDialog", "Failed to parse Wi-Fi SSID", e)
        }
    }

    private fun setupForText(text: String) {
        binding.dialogTitle.text = "Text Result"
        binding.dialogIcon.setImageResource(R.drawable.ic_qr_code_scanned)
        binding.copyButton.visibility = View.VISIBLE
        binding.shareButton.visibility = View.VISIBLE

        // Handle different text types with appropriate actions
        when {
            isEmailAddress(text) -> {
                binding.dialogTitle.text = "Email Address"
                binding.dialogIcon.setImageResource(R.drawable.ic_email)
                binding.openLinkButton.visibility = View.VISIBLE
                binding.openLinkButton.text = "Send Email"
            }
            isPhoneNumber(text) -> {
                binding.dialogTitle.text = "Phone Number"
                binding.dialogIcon.setImageResource(R.drawable.ic_phone)
                binding.openLinkButton.visibility = View.VISIBLE
                binding.openLinkButton.text = "Call"
            }
        }
    }

    // --- Action Functions ---
    private fun copyToClipboard(text: String) {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code Result", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            dismiss()
        } catch (e: Exception) {
            Log.e("SquareResultDialog", "Failed to copy to clipboard", e)
            Toast.makeText(requireContext(), "Failed to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareResult(text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Share QR Code Result")
            if (chooser.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(chooser)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "No app available to share", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SquareResultDialog", "Failed to share result", e)
            Toast.makeText(requireContext(), "Failed to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(content: String) {
        try {
            val intent = when {
                isEmailAddress(content) -> {
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$content")
                    }
                }
                isPhoneNumber(content) -> {
                    Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$content")
                    }
                }
                else -> {
                    // Handle URL
                    val url = if (!content.startsWith("http://") && !content.startsWith("https://")) {
                        "https://$content"
                    } else {
                        content
                    }
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                }
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "No app available to handle this", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SquareResultDialog", "Failed to open content", e)
            Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToWifi(wifiData: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val wifiInfo = parseWifiData(wifiData)
                if (wifiInfo.ssid.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalid Wi-Fi QR code format", Toast.LENGTH_SHORT).show()
                    return
                }

                val suggestionBuilder = WifiNetworkSuggestion.Builder().setSsid(wifiInfo.ssid)

                // Handle different security types
                when (wifiInfo.security.uppercase()) {
                    "WPA", "WPA2" -> {
                        if (wifiInfo.password.isNotEmpty()) {
                            suggestionBuilder.setWpa2Passphrase(wifiInfo.password)
                        }
                    }
                    "WEP" -> {
                        if (wifiInfo.password.isNotEmpty()) {
                            suggestionBuilder.setWpa2Passphrase(wifiInfo.password) // WEP not directly supported, fallback to WPA2
                        }
                    }
                    "NOPASS", "" -> {
                        // Open network, no password needed
                    }
                }

                val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
                val status = wifiManager.addNetworkSuggestions(listOf(suggestionBuilder.build()))

                when (status) {
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> {
                        Toast.makeText(requireContext(), "Wi-Fi network added successfully", Toast.LENGTH_SHORT).show()
                        // Open Wi-Fi settings for user to connect
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> {
                        Toast.makeText(requireContext(), "Network already exists", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    else -> {
                        Toast.makeText(requireContext(), "Failed to add Wi-Fi network", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("SquareResultDialog", "Failed to connect to Wi-Fi", e)
                Toast.makeText(requireContext(), "Invalid Wi-Fi QR code", Toast.LENGTH_SHORT).show()
            }
        } else {
            // For older Android versions, just copy the details and open settings
            copyToClipboard(wifiData)
            Toast.makeText(requireContext(), "Wi-Fi details copied. Please connect manually.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        dismiss()
    }

    // --- Helper Functions ---
    private fun parseWifiSsid(wifiData: String): String {
        return try {
            wifiData.substringAfter("S:").substringBefore(";").trim()
        } catch (e: Exception) {
            ""
        }
    }

    private data class WifiInfo(
        val ssid: String,
        val security: String,
        val password: String,
        val hidden: Boolean = false
    )

    private fun parseWifiData(wifiData: String): WifiInfo {
        try {
            // Format: WIFI:S:<SSID>;T:<WEP|WPA|nopass>;P:<PASSWORD>;H:<true|false>;;
            val ssid = wifiData.substringAfter("S:").substringBefore(";").trim()
            val security = wifiData.substringAfter("T:").substringBefore(";").trim()
            val password = wifiData.substringAfter("P:").substringBefore(";").trim()
            val hiddenStr = wifiData.substringAfter("H:").substringBefore(";").trim()
            val hidden = hiddenStr.equals("true", ignoreCase = true)

            return WifiInfo(ssid, security, password, hidden)
        } catch (e: Exception) {
            return WifiInfo("", "", "")
        }
    }

    private fun isEmailAddress(text: String): Boolean {
        return text.contains("@") && text.contains(".") &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()
    }

    private fun isPhoneNumber(text: String): Boolean {
        return android.util.Patterns.PHONE.matcher(text).matches()
    }

    companion object {
        private const val ARG_SCANNED_RESULT = "scanned_result"
        private const val ARG_QR_CODE_TYPE = "qr_code_type"

        fun newInstance(scannedResult: String, type: QrCodeType): SquareResultDialogFragment {
            return SquareResultDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SCANNED_RESULT, scannedResult)
                    putSerializable(ARG_QR_CODE_TYPE, type)
                }
            }
        }
    }
}