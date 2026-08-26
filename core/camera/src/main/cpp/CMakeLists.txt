cmake_minimum_required(VERSION 3.22.1)
project(arcface_ncnn)

set(NCNN_ROOT "${CMAKE_CURRENT_LIST_DIR}/third_party/ncnn/ncnn-20260526-android")
set(ncnn_DIR "${NCNN_ROOT}/${ANDROID_ABI}/lib/cmake/ncnn")
find_package(ncnn REQUIRED CONFIG)

add_library(arcface_ncnn SHARED arcface_ncnn.cpp)
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_compile_features(arcface_ncnn PRIVATE cxx_std_17)
target_link_libraries(arcface_ncnn ncnn ${log-lib} ${jnigraphics-lib})
