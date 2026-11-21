cmake_minimum_required(VERSION 3.22)
project(peroma_stt LANGUAGES C CXX)

# transcribe.cpp — the speech engine behind Handy (https://handy.computer) —
# is not vendored. app/build.gradle downloads the pinned release and passes its
# location in; the build fails loudly if that did not happen.
if(NOT TRANSCRIBE_SRC OR NOT EXISTS "${TRANSCRIBE_SRC}/CMakeLists.txt")
    message(FATAL_ERROR "TRANSCRIBE_SRC must point at the transcribe.cpp sources (set by Gradle)")
endif()

# transcribe.cpp's own build assumes it is the top-level project and locates its
# public headers through CMAKE_SOURCE_DIR. Pointing that at its own directory
# is the only change needed to build it as a subproject.
set(_tc_cmake "${TRANSCRIBE_SRC}/src/CMakeLists.txt")
file(READ "${_tc_cmake}" _tc_text)
if(_tc_text MATCHES "\\$\\{CMAKE_SOURCE_DIR\\}/include")
    string(REPLACE "\${CMAKE_SOURCE_DIR}/include" "\${PROJECT_SOURCE_DIR}/include" _tc_text "${_tc_text}")
    file(WRITE "${_tc_cmake}" "${_tc_text}")
endif()

# The engine is linked statically into our shared library, so it must be PIC.
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Static, CPU-only, no tests: one self-contained .so, nothing to load at run time.
set(TRANSCRIBE_BUILD_SHARED   OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(TRANSCRIBE_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)
add_subdirectory("${TRANSCRIBE_SRC}" transcribe EXCLUDE_FROM_ALL)

add_library(peroma_stt SHARED peroma_stt.cpp)
target_include_directories(peroma_stt PRIVATE "${TRANSCRIBE_SRC}/include")
target_link_libraries(peroma_stt PRIVATE transcribe)

# Outside Android — the host-side test in app/src/test builds the same library
# for the desktop JVM — jni.h comes from the JDK instead of the NDK.
if(NOT ANDROID)
    find_package(JNI REQUIRED)
    target_include_directories(peroma_stt PRIVATE ${JNI_INCLUDE_DIRS})
endif()

# Android 15 devices with 16 KB pages refuse libraries aligned to 4 KB.
target_link_options(peroma_stt PRIVATE -Wl,-z,max-page-size=16384)
