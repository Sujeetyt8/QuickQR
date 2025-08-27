package com.example.quickqr.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import java.io.*
import java.util.*

object FileUtils {
    
    private const val BUFFER_SIZE = 8192
    
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @WorkerThread
    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                
                // This is for checking Main Memory
                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }
                
                // This is for checking SD Card
                return "/storage/$docId".replace(":", "/")
                
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id.toLong()
                )
                return getDataColumn(context, contentUri, null, null)
                
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                
                return contentUri?.let {
                    getDataColumn(context, it, selection, selectionArgs)
                }
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            // MediaStore (and general)
            return getDataColumn(context, uri, null, null)
        } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            // File
            return uri.path
        }
        
        return null
    }
    
    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Files.FileColumns.DATA
        val projection = arrayOf(column)
        
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.let { 
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(column)
                    return it.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        return null
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
    
    /**
     * Get the file name from a content URI.
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex)
                    }
                }
            }
        }
        
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        
        return result
    }
    
    /**
     * Get the MIME type of a file.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> context.contentResolver.getType(uri)
            ContentResolver.SCHEME_FILE -> {
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
            }
            else -> null
        }
    }
    
    /**
     * Copy a file from source URI to destination file.
     */
    @Throws(IOException::class)
    fun copyFile(context: Context, sourceUri: Uri, destinationFile: File) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output, BUFFER_SIZE)
            }
        } ?: throw IOException("Failed to open input stream for $sourceUri")
    }
    
    /**
     * Create a temporary file in the cache directory.
     */
    @Throws(IOException::class)
    fun createTempFile(
        context: Context,
        prefix: String = "",
        suffix: String = ".tmp",
        directory: File = context.cacheDir
    ): File {
        return File.createTempFile(
            "${prefix}${System.currentTimeMillis()}",
            suffix,
            directory
        )
    }
    
    /**
     * Get the size of a file from URI.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            return pfd.statSize
        }
        return 0L
    }
    
    /**
     * Check if external storage is available for read and write.
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Check if external storage is available to at least read.
     */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }
    
    /**
     * Get the extension of a file.
     */
    fun getFileExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot == -1) "" else name.substring(lastDot + 1)
    }
    
    /**
     * Get the extension from a MIME type.
     */
    fun getExtensionFromMimeType(mimeType: String?): String? {
        return mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    }
    
    /**
     * Get the MIME type from a file extension.
     */
    fun getMimeTypeFromExtension(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
    }
}
