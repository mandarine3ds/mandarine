// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package io.github.mandarine3ds.mandarine.features.settings.model

enum class BooleanSetting(
    override val key: String,
    override val section: String,
    override val defaultValue: Boolean
) : AbstractBooleanSetting {
    EXPAND_TO_CUTOUT_AREA("expand_to_cutout_area", Settings.SECTION_LAYOUT, false),
    SPIRV_SHADER_GEN("spirv_shader_gen", Settings.SECTION_RENDERER, true),
    ASYNC_SHADERS("async_shader_compilation", Settings.SECTION_RENDERER, false),
    ADRENO_GPU_BOOST("adreno_gpu_boost", Settings.SECTION_RENDERER, false),
    PLUGIN_LOADER("plugin_loader", Settings.SECTION_SYSTEM, false),
    ALLOW_PLUGIN_LOADER("allow_plugin_loader", Settings.SECTION_SYSTEM, true),
    SWAP_SCREEN("swap_screen", Settings.SECTION_LAYOUT, false),
    CUSTOM_LAYOUT("custom_layout",Settings.SECTION_LAYOUT,false),
    INSTANT_DEBUG_LOG("instant_debug_log", Settings.SECTION_DEBUG, true),
    SHOW_FPS("show_fps", Settings.SECTION_LAYOUT, true),
    SHOW_SPEED("show_speed", Settings.SECTION_LAYOUT, false),
    SHOW_APP_RAM_USAGE("show_app_ram_usage", Settings.SECTION_LAYOUT, false),
    SHOW_SYSTEM_RAM_USAGE("show_system_ram_usage", Settings.SECTION_LAYOUT, false),
    SHOW_BAT_TEMPERATURE("show_bat_temperature", Settings.SECTION_LAYOUT, false),
    OVERLAY_BACKGROUND("overlay_background", Settings.SECTION_LAYOUT, false);

    override var boolean: Boolean = defaultValue

    override val valueAsString: String
        get() = boolean.toString()

    override val isRuntimeEditable: Boolean
        get() {
            for (setting in NOT_RUNTIME_EDITABLE) {
                if (setting == this) {
                    return false
                }
            }
            return true
        }

    companion object {
        private val NOT_RUNTIME_EDITABLE = listOf(
            ASYNC_SHADERS,
            ADRENO_GPU_BOOST,
            PLUGIN_LOADER,
            ALLOW_PLUGIN_LOADER
        )

        fun from(key: String): BooleanSetting? =
            BooleanSetting.values().firstOrNull { it.key == key }

        fun clear() = BooleanSetting.values().forEach { it.boolean = it.defaultValue }
    }
}
