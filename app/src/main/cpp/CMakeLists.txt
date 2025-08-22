cmake_minimum_required(VERSION 3.22.1)
project("whisper-android")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ARM v8 specific optimizations
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a")
    add_definitions(-DGGML_USE_FP16_VA=0)  # Disable FP16 for compatibility
    add_definitions(-DARM_NEON=1)
    add_definitions(-DGGML_USE_ACCELERATE=0)  # Disable Accelerate framework
    add_definitions(-DGGML_USE_OPENBLAS=0)
    add_definitions(-DGGML_USE_OPENMP=0)  # Disable OpenMP for Android
endif()

# Whisper.cpp configuration - disable problematic features
set(WHISPER_NO_AVX ON)
set(WHISPER_NO_AVX2 ON)
set(WHISPER_NO_AVX512 ON)
set(WHISPER_NO_FMA ON)
set(WHISPER_NO_F16C ON)
set(WHISPER_BUILD_TESTS OFF)
set(WHISPER_BUILD_EXAMPLES OFF)
set(GGML_USE_ACCELERATE OFF)
set(GGML_USE_OPENBLAS OFF)
set(GGML_USE_OPENMP OFF)

# TODO: Add whisper.cpp subdirectory when Accelerate issue is resolved
# add_subdirectory(${CMAKE_SOURCE_DIR}/whisper.cpp)

# Create whisper JNI shared library (placeholder for now)
add_library(whisper-jni SHARED
    whisper_jni_placeholder.cpp
    audio_processor.cpp
)

# Link libraries
target_link_libraries(whisper-jni
    # whisper  # TODO: Enable when whisper.cpp is working
    log
    android
)

# Include directories
target_include_directories(whisper-jni PRIVATE
    ${CMAKE_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/whisper.cpp/include
    ${CMAKE_SOURCE_DIR}/whisper.cpp/src
)
