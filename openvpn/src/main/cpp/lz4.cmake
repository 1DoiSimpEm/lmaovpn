# Check if lz4 source exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/lz4/lib/lz4.c")
# Check if lz4 source exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/lz4/lib/lz4.c")
    set(lz4_srcs
      lz4.c
      )

    PREPEND(lz4_src_with_path "lz4/lib/" ${lz4_srcs})
    add_library(lz4 ${lz4_src_with_path})
    target_include_directories(lz4 PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lz4/lib")
else()
    message(WARNING "lz4/lib/lz4.c not found - creating stub library")
    # Create a stub library to allow CMake configuration to proceed
    file(WRITE ${CMAKE_BINARY_DIR}/lz4_stub.c "// lz4 stub\n")
    add_library(lz4 STATIC ${CMAKE_BINARY_DIR}/lz4_stub.c)
    target_include_directories(lz4 PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lz4/lib")
endif()
else()
    message(WARNING "lz4/lib/lz4.c not found - creating stub library")
    # Create a stub library to allow CMake configuration to proceed
    file(WRITE ${CMAKE_BINARY_DIR}/lz4_stub.c "// lz4 stub\n")
    add_library(lz4 STATIC ${CMAKE_BINARY_DIR}/lz4_stub.c)
    target_include_directories(lz4 PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lz4/lib")
endif()
