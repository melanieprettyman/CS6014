cmake_minimum_required(VERSION 3.27)
project(RC4)

set(CMAKE_CXX_STANDARD 17)

add_executable(RC4 main.cpp
        RC4.cpp
        RC4.h)
