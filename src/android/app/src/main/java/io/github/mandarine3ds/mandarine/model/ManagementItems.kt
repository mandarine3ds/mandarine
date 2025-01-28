// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright 2025 Mandarine Project


package io.github.mandarine3ds.mandarine.model


import androidx.annotation.StringRes
import android.net.Uri

data class ManagementItems(
    @StringRes val titleId: Int, // Card title
    @StringRes val descriptionId: Int, // Card description
    val set: (() -> Unit)? = null, // Set folder
    val install: (() -> Unit)? = null, // Install game
    var directories: List<Uri>? = null, // Search location
    val onDeleteDirectory: ((Uri) -> Unit)? = null, // Search location
    val details: String? = null  // For displaying user directory path
)