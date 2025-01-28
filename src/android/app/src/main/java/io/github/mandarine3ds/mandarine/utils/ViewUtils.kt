// Copyright 2025 Citra / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.utils

import android.view.View

object ViewUtils {
    fun showView(view: View, length: Long = 300) {
        view.apply {
            alpha = 0f
            visibility = View.VISIBLE
            isClickable = true
        }.animate().apply {
            duration = length
            alpha(1f)
        }.start()
    }

    /**
     * Shows or hides a view.
     * @param visible Whether a view will be made View.VISIBLE or View.INVISIBLE/GONE.
     * @param gone Optional parameter for hiding a view. Uses View.GONE if true and View.INVISIBLE otherwise.
     */
    fun View.setVisible(visible: Boolean, gone: Boolean = true) {
        visibility = if (visible) {
            View.VISIBLE
        } else {
            if (gone) {
                View.GONE
            } else {
                View.INVISIBLE
            }
        }
    }

    fun hideView(view: View, length: Long = 300) {
        if (view.visibility == View.INVISIBLE) {
            return
        }

        view.apply {
            alpha = 1f
            isClickable = false
        }.animate().apply {
            duration = length
            alpha(0f)
        }.withEndAction {
            view.visibility = View.INVISIBLE
        }.start()
    }
}
