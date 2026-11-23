cmake_minimum_required(VERSION 3.22.1)
project(usbaudio_driver VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    usbaudio_driver
    SHARED
    jni_bridge.cpp
    spsc_ring_buffer.cpp
    descriptor_parser.cpp
    usb_stream_engine.cpp
)

target_include_directories(
    usbaudio_driver
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(
    usbaudio_driver
    ${log-lib}
)
