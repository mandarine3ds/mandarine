set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${PROJECT_SOURCE_DIR}/CMakeModules)

add_executable(mandarine
    mandarine.cpp
    mandarine.rc
    config.cpp
    config.h
    default_ini.h
    emu_window/emu_window_sdl2.cpp
    emu_window/emu_window_sdl2.h
    precompiled_headers.h
    resource.h
)

if (ENABLE_SOFTWARE_RENDERER)
    target_sources(mandarine PRIVATE
        emu_window/emu_window_sdl2_sw.cpp
        emu_window/emu_window_sdl2_sw.h
    )
endif()
if (ENABLE_OPENGL)
    target_sources(mandarine PRIVATE
        emu_window/emu_window_sdl2_gl.cpp
        emu_window/emu_window_sdl2_gl.h
    )
endif()
if (ENABLE_VULKAN)
    target_sources(mandarine PRIVATE
        emu_window/emu_window_sdl2_vk.cpp
        emu_window/emu_window_sdl2_vk.h
    )
endif()

create_target_directory_groups(mandarine)

target_link_libraries(mandarine PRIVATE mandarine_common mandarine_core input_common network)
target_link_libraries(mandarine PRIVATE inih)
if (MSVC)
    target_link_libraries(mandarine PRIVATE getopt)
endif()
target_link_libraries(mandarine PRIVATE ${PLATFORM_LIBRARIES} SDL2::SDL2 Threads::Threads)

if (ENABLE_OPENGL)
    target_link_libraries(mandarine PRIVATE glad)
endif()

if(UNIX AND NOT APPLE)
    install(TARGETS mandarine RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}/bin")
endif()

if (MANDARINE_USE_PRECOMPILED_HEADERS)
    target_precompile_headers(mandarine PRIVATE precompiled_headers.h)
endif()

# Bundle in-place on MSVC so dependencies can be resolved by builds.
if (MSVC)
    include(BundleTarget)
    bundle_target_in_place(mandarine)
endif()
