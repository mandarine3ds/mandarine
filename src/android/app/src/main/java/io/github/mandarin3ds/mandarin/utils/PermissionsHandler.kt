// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarin3ds.mandarin.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.documentfile.provider.DocumentFile
import io.github.mandarin3ds.mandarin.MandarinApplication

object PermissionsHandler {
    const val MANDARIN_DIRECTORY = "MANDARIN_DIRECTORY"
    val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(MandarinApplication.appContext)

    fun hasWriteAccess(context: Context): Boolean {
        try {
            if (mandarinDirectory.toString().isEmpty()) {
                return false
            }

            val uri = mandarinDirectory
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            val root = DocumentFile.fromTreeUri(context, uri)
            if (root != null && root.exists()) {
                return true
            }

            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            Log.error("[PermissionsHandler]: Cannot check mandarin data directory permission, error: " + e.message)
        }
        return false
    }

    val mandarinDirectory: Uri
        get() {
            val directoryString = preferences.getString(MANDARIN_DIRECTORY, "")
            return Uri.parse(directoryString)
        }

    fun setMandarinDirectory(uriString: String?) =
        preferences.edit().putString(MANDARIN_DIRECTORY, uriString).apply()
}
