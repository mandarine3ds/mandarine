// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.documentfile.provider.DocumentFile
import io.github.mandarine3ds.mandarine.MandarineApplication

object PermissionsHandler {
    const val MANDARINE_DIRECTORY = "MANDARINE_DIRECTORY"
    val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(MandarineApplication.appContext)

    fun hasWriteAccess(context: Context): Boolean {
        try {
            if (mandarineDirectory.toString().isEmpty()) {
                return false
            }

            val uri = mandarineDirectory
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            val root = DocumentFile.fromTreeUri(context, uri)
            if (root != null && root.exists()) {
                return true
            }

            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            Log.error("[PermissionsHandler]: Cannot check mandarine data directory permission, error: " + e.message)
        }
        return false
    }

    val mandarineDirectory: Uri
        get() {
            val directoryString = preferences.getString(MANDARINE_DIRECTORY, "")
            return Uri.parse(directoryString)
        }

    fun setMandarineDirectory(uriString: String?) =
        preferences.edit().putString(MANDARINE_DIRECTORY, uriString).apply()
}
