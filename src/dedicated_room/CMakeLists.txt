set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${PROJECT_SOURCE_DIR}/CMakeModules)

add_executable(mandarine-room
    precompiled_headers.h
    mandarine-room.cpp
    mandarine-room.rc
)

create_target_directory_groups(mandarine-room)

target_link_libraries(mandarine-room PRIVATE mandarine_common network)
if (ENABLE_WEB_SERVICE)
    target_link_libraries(mandarine-room PRIVATE web_service)
endif()

target_link_libraries(mandarine-room PRIVATE cryptopp)
if (MSVC)
    target_link_libraries(mandarine-room PRIVATE getopt)
endif()
target_link_libraries(mandarine-room PRIVATE ${PLATFORM_LIBRARIES} Threads::Threads)

if(UNIX AND NOT APPLE)
    install(TARGETS mandarine-room RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}/bin")
endif()

if (MANDARINE_USE_PRECOMPILED_HEADERS)
    target_precompile_headers(mandarine-room PRIVATE precompiled_headers.h)
endif()

# Bundle in-place on MSVC so dependencies can be resolved by builds.
if (MSVC)
    include(BundleTarget)
    bundle_target_in_place(mandarine-room)
endif()
