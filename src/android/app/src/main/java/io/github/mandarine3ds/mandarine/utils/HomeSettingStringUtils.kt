
package io.github.mandarine3ds.mandarine.utils

sealed class HomeSettingStringUtils {
    data class Text(val value: String) : HomeSettingStringUtils()
    data class ResId(val id: Int) : HomeSettingStringUtils()
}
