cmake_minimum_required(VERSION 3.22.1)

project(thor_uinput_probe)

add_library(thor_uinput_probe SHARED
        uinput_probe.cpp)

find_library(log-lib log)

target_link_libraries(thor_uinput_probe
        ${log-lib})
