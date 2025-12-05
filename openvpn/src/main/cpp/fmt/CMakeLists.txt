# fmt library CMake configuration
# This is a stub file - replace with actual fmt library
# fmt is a formatting library: https://github.com/fmtlib/fmt

cmake_minimum_required(VERSION 3.4.1)

project(fmt)

# Create a stub library to allow CMake configuration to proceed
# This will need to be replaced with the actual fmt library for the build to succeed
message(WARNING "fmt library not found - creating stub. Build will fail until proper fmt is configured.")

# Create a stub library with a dummy source file
file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/fmt_stub.cpp "// fmt stub\nnamespace fmt {} // Empty namespace\n")
add_library(fmt STATIC ${CMAKE_CURRENT_BINARY_DIR}/fmt_stub.cpp)

# Set minimal properties
target_include_directories(fmt PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

