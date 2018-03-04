cmake_minimum_required(VERSION 3.8.0)

project (sbe-encode-decode)

set(STUBS
  "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/test/resources/projects/generator/build/archives/test-name-1.2.3-cpp-cmake.tar.gz")

set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)
set(APP ${PROJECT_NAME})

include(ExternalProject)

ExternalProject_Add(sbetest
  URL     ${STUBS}
  URL_MD5 19e9b3c3f85436be84fb7ec6e6cbda35

# Optional
  SOURCE_DIR        "${CMAKE_CURRENT_BINARY_DIR}/sbetest-src"
  BINARY_DIR        "${CMAKE_CURRENT_BINARY_DIR}/sbetest-build"

  UPDATE_COMMAND    ""
  CONFIGURE_COMMAND ""
  BUILD_COMMAND     ""
  TEST_COMMAND      ""
  INSTALL_COMMAND   ""
)

ExternalProject_Get_Property(sbetest source_dir)
set(SBETEST_INCLUDE "${source_dir}/include")

add_executable(${APP} "src/main.cpp")
target_include_directories(${APP} PRIVATE "${SBETEST_INCLUDE}")
