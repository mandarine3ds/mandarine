#!/bin/bash -ex

if [ "$TARGET" = "appimage" ]; then
    export EXTRA_CMAKE_FLAGS=(-DCMAKE_LINKER=/etc/bin/ld.lld)
else
    # For the linux-fresh verification target, verify compilation without PCH as well.
    export EXTRA_CMAKE_FLAGS=(-DMANDARINE_USE_PRECOMPILED_HEADERS=OFF)
fi

mkdir build && cd build
cmake .. -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_COMPILER_LAUNCHER=ccache \
    -DCMAKE_CXX_COMPILER_LAUNCHER=ccache \
    -DCMAKE_CXX_COMPILER=clang++-18 \
    -DCMAKE_C_COMPILER=clang-18 \
    "${EXTRA_CMAKE_FLAGS[@]}" \
    -DENABLE_QT_TRANSLATION=ON \
    -DUSE_DISCORD_PRESENCE=ON
ninja

if [ "$TARGET" = "appimage" ]; then
    ninja bundle
    # TODO: Our AppImage environment currently uses an older ccache version without the verbose flag.
    ccache -s
else
    ccache -s -v
fi

ctest -VV -C Release
