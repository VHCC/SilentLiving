# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(libs "${CMAKE_SOURCE_DIR}/../jniLibs")
include_directories(${CMAKE_SOURCE_DIR}/include)
include_directories(${CMAKE_SOURCE_DIR}/include/seeta)
include_directories(${CMAKE_SOURCE_DIR}/include/seeta/Common)
include_directories(${CMAKE_SOURCE_DIR}/include/opencv2)
#include_directories(${CMAKE_SOURCE_DIR}/include/opencv)

#add_library(libopencv_java3 SHARED IMPORTED )
#set_target_properties(libopencv_java3 PROPERTIES
#        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_java3.so")

#add_library(libseetanet2 SHARED IMPORTED )
#set_target_properties(libseetanet2 PROPERTIES
#        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libseetanet2.so")

#add_library(libfd SHARED IMPORTED )
#set_target_properties(libfd PROPERTIES
#        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libSeetaFaceDetector2.so")

#add_library(libfl SHARED IMPORTED )
#set_target_properties(libfl PROPERTIES
#        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libSeetaFaceLandmarker2.so")

#add_library(libfr SHARED IMPORTED )
#set_target_properties(libfr PROPERTIES
#        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libSeetaFaceRecognizer2.so")

find_library( # Sets the name of the path variable.
        jnigraphics-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        jnigraphics)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)


add_library( # Sets the name of the library.
        SeetaAiCSFaceDetector

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        facedetector_java.cpp)

target_link_libraries( # Specifies the target library.
        SeetaAiCSFaceDetector android
        libopencv_java3
        libseetanet2
        libfd
        libfl
        libfr
        ${log-lib})