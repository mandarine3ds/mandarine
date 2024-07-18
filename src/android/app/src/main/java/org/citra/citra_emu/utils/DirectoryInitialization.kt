// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package org.citra.citra_emu.utils

import android.content.Context
import android.net.Uri
import org.citra.citra_emu.CitraApplication
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.utils.PermissionsHandler.hasWriteAccess
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A service that spawns its own thread in order to copy several binary and shader files
 * from the Citra APK to the external file system.
 */
object DirectoryInitialization {
    @Volatile
    private var directoryState: DirectoryInitializationState? = null
    var userPath: String? = null
    val internalUserPath
        get() = CitraApplication.appContext.getExternalFilesDir(null)!!.canonicalPath
    private val isCitraDirectoryInitializationRunning = AtomicBoolean(false)

    val context: Context get() = CitraApplication.appContext

    @JvmStatic
    fun start(): DirectoryInitializationState? {
        if (!isCitraDirectoryInitializationRunning.compareAndSet(false, true)) {
            return null
        }

        if (directoryState != DirectoryInitializationState.CITRA_DIRECTORIES_INITIALIZED) {
            directoryState = if (hasWriteAccess(context)) {
                if (setCitraUserDirectory()) {
                    CitraApplication.documentsTree.setRoot(Uri.parse(userPath))
                    NativeLibrary.createLogFile()
                    NativeLibrary.logUserDirectory(userPath.toString())
                    NativeLibrary.createConfigFile()
                    GpuDriverHelper.initializeDriverParameters()
                    DirectoryInitializationState.CITRA_DIRECTORIES_INITIALIZED
                } else {
                    DirectoryInitializationState.CANT_FIND_EXTERNAL_STORAGE
                }
            } else {
                DirectoryInitializationState.EXTERNAL_STORAGE_PERMISSION_NEEDED
            }
        }
        isCitraDirectoryInitializationRunning.set(false)
        return directoryState
    }

    @JvmStatic
    fun areCitraDirectoriesReady(): Boolean {
        return directoryState == DirectoryInitializationState.CITRA_DIRECTORIES_INITIALIZED
    }

    fun resetCitraDirectoryState() {
        directoryState = null
        isCitraDirectoryInitializationRunning.compareAndSet(true, false)
    }

    val userDirectory: String?
        get() {
            checkNotNull(directoryState) {
                "DirectoryInitialization has to run at least once!"
            }
            check(!isCitraDirectoryInitializationRunning.get()) {
                "DirectoryInitialization has to finish running first!"
            }
            return userPath
        }

    fun setCitraUserDirectory(): Boolean {
        val dataPath = PermissionsHandler.citraDirectory
        if (dataPath.toString().isNotEmpty()) {
            userPath = dataPath.toString()
            android.util.Log.d("[Citra Frontend]", "[DirectoryInitialization] User Dir: $userPath")
            return true
        }
        return false
    }

    enum class DirectoryInitializationState {
        CITRA_DIRECTORIES_INITIALIZED,
        EXTERNAL_STORAGE_PERMISSION_NEEDED,
        CANT_FIND_EXTERNAL_STORAGE
    }
}
