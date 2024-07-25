#include "mica.h"

#ifdef _WIN32
#include <dwmapi.h>

namespace Utils {

void EnableDarkMicaForWindow(HWND hwnd) {
    // Enable dark mode for the title bar (undocumented feature)
    BOOL darkMode = TRUE;
    DwmSetWindowAttribute(hwnd, 20, &darkMode,
                          sizeof(darkMode)); // 20 corresponds to DWMWA_USE_IMMERSIVE_DARK_MODE

    // Apply Mica effect
    DWMWINDOWATTRIBUTE attribute = DWMWA_SYSTEMBACKDROP_TYPE;
    DWORD attributeValue = DWMSBT_MAINWINDOW;
    DwmSetWindowAttribute(hwnd, attribute, &attributeValue, sizeof(attributeValue));
}

} // namespace Utils

#endif
