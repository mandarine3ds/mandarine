// Copyright 2025 Citra Project / Mandarine Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#pragma once

#include <jni.h>
#include "core/frontend/applets/swkbd.h"

namespace SoftwareKeyboard {

class AndroidKeyboard final : public Frontend::SoftwareKeyboard {
public:
    ~AndroidKeyboard();

    void Execute(const Frontend::KeyboardConfig& config) override;
    void ShowError(const std::string& error) override;
};

// Should be called in JNI_Load
void InitJNI(JNIEnv* env);

// Should be called in JNI_Unload
void CleanupJNI(JNIEnv* env);

} // namespace SoftwareKeyboard

// Native function calls
extern "C" {
JNIEXPORT jobject JNICALL
Java_io_github_mandarine3ds_mandarine_applets_SoftwareKeyboard_validateInput(JNIEnv* env,
                                                                             jclass clazz,
                                                                             jstring text);

JNIEXPORT jobject JNICALL
Java_io_github_mandarine3ds_mandarine_applets_SoftwareKeyboard_validateInput(JNIEnv* env,
                                                                             jclass clazz,
                                                                             jstring text);
}
