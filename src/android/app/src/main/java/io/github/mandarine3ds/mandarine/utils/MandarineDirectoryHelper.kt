// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import io.github.mandarine3ds.mandarine.fragments.MandarineDirectoryDialogFragment
import io.github.mandarine3ds.mandarine.fragments.CopyDirProgressDialog
import io.github.mandarine3ds.mandarine.model.SetupCallback
import io.github.mandarine3ds.mandarine.viewmodel.HomeViewModel

/**
 * Mandarine directory initialization ui flow controller.
 */
class MandarineDirectoryHelper(private val fragmentActivity: FragmentActivity) {
    fun showMandarineDirectoryDialog(result: Uri, callback: SetupCallback? = null) {
        val mandarineDirectoryDialog = MandarineDirectoryDialogFragment.newInstance(
            fragmentActivity,
            result.toString(),
            MandarineDirectoryDialogFragment.Listener { moveData: Boolean, path: Uri ->
                val previous = PermissionsHandler.mandarineDirectory
                // Do noting if user select the previous path.
                if (path == previous) {
                    return@Listener
                }

                val takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                fragmentActivity.contentResolver.takePersistableUriPermission(
                    path,
                    takeFlags
                )
                if (!moveData || previous.toString().isEmpty()) {
                    initializeMandarineDirectory(path)
                    callback?.onStepCompleted()
                    val viewModel = ViewModelProvider(fragmentActivity)[HomeViewModel::class.java]
                    viewModel.setUserDir(fragmentActivity, path.path!!)
                    viewModel.setPickingUserDir(false)
                    return@Listener
                }

                // If user check move data, show copy progress dialog.
                CopyDirProgressDialog.newInstance(fragmentActivity, previous, path, callback)
                    ?.show(fragmentActivity.supportFragmentManager, CopyDirProgressDialog.TAG)
            })
        mandarineDirectoryDialog.show(
            fragmentActivity.supportFragmentManager,
            MandarineDirectoryDialogFragment.TAG
        )
    }

    companion object {
        fun initializeMandarineDirectory(path: Uri) {
            PermissionsHandler.setMandarineDirectory(path.toString())
            DirectoryInitialization.resetMandarineDirectoryState()
            DirectoryInitialization.start()
        }
    }
}
