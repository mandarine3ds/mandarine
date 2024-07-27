// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import android.content.Context
import android.net.Uri
import io.github.mandarine3ds.mandarine.MandarineApplication
import io.github.mandarine3ds.mandarine.NativeLibrary
import io.github.mandarine3ds.mandarine.utils.PermissionsHandler.hasWriteAccess
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A service that spawns its own thread in order to copy several binary and shader files
 * from the Mandarine APK to the external file system.
 */
object DirectoryInitialization {
    @Volatile
    private var directoryState: DirectoryInitializationState? = null
    var userPath: String? = null
    val internalUserPath
        get() = MandarineApplication.appContext.getExternalFilesDir(null)!!.canonicalPath
    private val isMandarineDirectoryInitializationRunning = AtomicBoolean(false)

    val context: Context get() = MandarineApplication.appContext

    @JvmStatic
    fun start(): DirectoryInitializationState? {
        if (!isMandarineDirectoryInitializationRunning.compareAndSet(false, true)) {
            return null
        }

        if (directoryState != DirectoryInitializationState.MANDARINE_DIRECTORIES_INITIALIZED) {
            directoryState = if (hasWriteAccess(context)) {
                if (setMandarineUserDirectory()) {
                    MandarineApplication.documentsTree.setRoot(Uri.parse(userPath))
                    NativeLibrary.createLogFile()
                    NativeLibrary.logUserDirectory(userPath.toString())
                    NativeLibrary.createConfigFile()
                    GpuDriverHelper.initializeDriverParameters()
                    DirectoryInitializationState.MANDARINE_DIRECTORIES_INITIALIZED
                } else {
                    DirectoryInitializationState.CANT_FIND_EXTERNAL_STORAGE
                }
            } else {
                DirectoryInitializationState.EXTERNAL_STORAGE_PERMISSION_NEEDED
            }
        }
        isMandarineDirectoryInitializationRunning.set(false)
        return directoryState
    }

    @JvmStatic
    fun areMandarineDirectoriesReady(): Boolean {
        return directoryState == DirectoryInitializationState.MANDARINE_DIRECTORIES_INITIALIZED
    }

    fun resetMandarineDirectoryState() {
        directoryState = null
        isMandarineDirectoryInitializationRunning.compareAndSet(true, false)
    }

    val userDirectory: String?
        get() {
            checkNotNull(directoryState) {
                "DirectoryInitialization has to run at least once!"
            }
            check(!isMandarineDirectoryInitializationRunning.get()) {
                "DirectoryInitialization has to finish running first!"
            }
            return userPath
        }

    fun setMandarineUserDirectory(): Boolean {
        val dataPath = PermissionsHandler.mandarineDirectory
        if (dataPath.toString().isNotEmpty()) {
            userPath = dataPath.toString()
            android.util.Log.d("[Mandarine Frontend]", "[DirectoryInitialization] User Dir: $userPath")
            return true
        }
        return false
    }

    enum class DirectoryInitializationState {
        MANDARINE_DIRECTORIES_INITIALIZED,
        EXTERNAL_STORAGE_PERMISSION_NEEDED,
        CANT_FIND_EXTERNAL_STORAGE
    }
}
