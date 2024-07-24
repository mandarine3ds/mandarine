set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${PROJECT_SOURCE_DIR}/CMakeModules)

add_executable(mandarin-room
    precompiled_headers.h
    mandarin-room.cpp
    mandarin-room.rc
)

create_target_directory_groups(mandarin-room)

target_link_libraries(mandarin-room PRIVATE mandarin_common network)
if (ENABLE_WEB_SERVICE)
    target_link_libraries(mandarin-room PRIVATE web_service)
endif()

target_link_libraries(mandarin-room PRIVATE cryptopp)
if (MSVC)
    target_link_libraries(mandarin-room PRIVATE getopt)
endif()
target_link_libraries(mandarin-room PRIVATE ${PLATFORM_LIBRARIES} Threads::Threads)

if(UNIX AND NOT APPLE)
    install(TARGETS mandarin-room RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}/bin")
endif()

if (MANDARIN_USE_PRECOMPILED_HEADERS)
    target_precompile_headers(mandarin-room PRIVATE precompiled_headers.h)
endif()

# Bundle in-place on MSVC so dependencies can be resolved by builds.
if (MSVC)
    include(BundleTarget)
    bundle_target_in_place(mandarin-room)
endif()
