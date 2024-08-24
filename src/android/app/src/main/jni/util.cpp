// Copyright 2024 Lime3DS Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include "jni/id_cache.h"

bool IsPortraitMode() {
    return JNI_FALSE != IDCache::GetEnvForThread()->CallStaticBooleanMethod(
                            IDCache::GetNativeLibraryClass(), IDCache::GetIsPortraitMode());
}
