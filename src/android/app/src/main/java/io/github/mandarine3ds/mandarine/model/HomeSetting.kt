// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.github.mandarine3ds.mandarine.utils.HomeSettingStringUtils

data class HomeSetting(
    val title: HomeSettingStringUtils,
    val description: HomeSettingStringUtils,
    val iconId: Int,
    val onClick: () -> Unit,
    val isEnabled: () -> Boolean = { true },
    val disabledTitleId: Int = 0,
    val disabledMessageId: Int = 0,
    val details: StateFlow<String> = MutableStateFlow("")
)
