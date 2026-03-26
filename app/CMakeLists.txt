cmake_minimum_required(VERSION 3.4.1)
add_library(uusd_jni SHARED src/main/cpp/uusd_jni.cpp)
find_library(log-lib log)
target_link_libraries(uusd_jni ${log-lib})

#uusdmainactivity

#cmake_minimum_required(VERSION 3.18.1)
#
#project("ussd_executor")
#
#set(CMAKE_CXX_STANDARD 11)
#set(CMAKE_CXX_STANDARD_REQUIRED ON)
#
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
#
#find_library(log-lib log)
#find_library(android-lib android)
#
#add_library(
#        ussd_executor
#        SHARED
#        src/main/cpp/ussd_executor.cpp
#)
#
#target_link_libraries(
#        ussd_executor
#        ${log-lib}
#        ${android-lib}
#        dl
#)
#
#target_compile_definitions(ussd_executor PRIVATE
#        ANDROID_NDK
#        HAVE_PTHREADS
#        )
#
#target_include_directories(ussd_executor PRIVATE
#        ${CMAKE_CURRENT_SOURCE_DIR}
#        )