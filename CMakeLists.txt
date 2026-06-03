cmake_minimum_required(VERSION 3.22.1)
project(crypto_core)

set(CMAKE_CXX_STANDARD 17)

# OpenSSL — bundle prebuilt or pull via prefab
find_library(log-lib       log)
find_library(android-lib   android)

# If using bundled OpenSSL:
set(OPENSSL_ROOT ${CMAKE_SOURCE_DIR}/src/main/cpp/openssl/${ANDROID_ABI})
include_directories(${OPENSSL_ROOT}/include)

add_library(
    crypto_core
    SHARED
    src/main/cpp/crypto_core.cpp
)

target_link_libraries(
    crypto_core
    ${OPENSSL_ROOT}/lib/libssl.a
    ${OPENSSL_ROOT}/lib/libcrypto.a
    ${log-lib}
    ${android-lib}
)
