// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.display

import android.view.WindowManager
import io.github.mandarine3ds.mandarine.NativeLibrary
import io.github.mandarine3ds.mandarine.features.settings.model.BooleanSetting
import io.github.mandarine3ds.mandarine.features.settings.model.IntSetting
import io.github.mandarine3ds.mandarine.features.settings.model.Settings
import io.github.mandarine3ds.mandarine.features.settings.utils.SettingsFile
import io.github.mandarine3ds.mandarine.utils.EmulationMenuSettings

class ScreenAdjustmentUtil(private val windowManager: WindowManager,
                           private val settings: Settings) {
    fun swapScreen() {
        val isEnabled = !EmulationMenuSettings.swapScreens
        EmulationMenuSettings.swapScreens = isEnabled
        NativeLibrary.swapScreens(
            isEnabled,
            windowManager.defaultDisplay.rotation
        )
        BooleanSetting.SWAP_SCREEN.boolean = isEnabled
        settings.saveSetting(BooleanSetting.SWAP_SCREEN, SettingsFile.FILE_NAME_CONFIG)
    }

    fun cycleLayouts() {
        // TODO: figure out how to pull these from R.array
        val landscapeValues = intArrayOf(6, 1, 3, 4, 0, 5)
        val portraitValues = intArrayOf(0, 1)
        if (NativeLibrary.isPortraitMode) {
            val currentLayout = IntSetting.PORTRAIT_SCREEN_LAYOUT.int
            val pos = portraitValues.indexOf(currentLayout)
            val layoutOption = portraitValues[(pos + 1) % portraitValues.size]
            changePortraitOrientation(layoutOption)
        } else {
            val currentLayout = IntSetting.SCREEN_LAYOUT.int
            val pos = landscapeValues.indexOf(currentLayout)
            val layoutOption = landscapeValues[(pos + 1) % landscapeValues.size]
            changeScreenOrientation(layoutOption)
        }
    }

    fun changePortraitOrientation(layoutOption: Int) {
        IntSetting.PORTRAIT_SCREEN_LAYOUT.int = layoutOption
        settings.saveSetting(IntSetting.PORTRAIT_SCREEN_LAYOUT, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode)
    }

    fun changeScreenOrientation(layoutOption: Int) {
        IntSetting.SCREEN_LAYOUT.int = layoutOption
        settings.saveSetting(IntSetting.SCREEN_LAYOUT, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode)
    }
}
