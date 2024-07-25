// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarin3ds.mandarin.utils

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import io.github.mandarin3ds.mandarin.fragments.MandarinDirectoryDialogFragment
import io.github.mandarin3ds.mandarin.fragments.CopyDirProgressDialog
import io.github.mandarin3ds.mandarin.model.SetupCallback
import io.github.mandarin3ds.mandarin.viewmodel.HomeViewModel

/**
 * Mandarin directory initialization ui flow controller.
 */
class MandarinDirectoryHelper(private val fragmentActivity: FragmentActivity) {
    fun showMandarinDirectoryDialog(result: Uri, callback: SetupCallback? = null) {
        val mandarinDirectoryDialog = MandarinDirectoryDialogFragment.newInstance(
            fragmentActivity,
            result.toString(),
            MandarinDirectoryDialogFragment.Listener { moveData: Boolean, path: Uri ->
                val previous = PermissionsHandler.mandarinDirectory
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
                    initializeMandarinDirectory(path)
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
        mandarinDirectoryDialog.show(
            fragmentActivity.supportFragmentManager,
            MandarinDirectoryDialogFragment.TAG
        )
    }

    companion object {
        fun initializeMandarinDirectory(path: Uri) {
            PermissionsHandler.setMandarinDirectory(path.toString())
            DirectoryInitialization.resetMandarinDirectoryState()
            DirectoryInitialization.start()
        }
    }
}
