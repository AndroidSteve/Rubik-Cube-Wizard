LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

APP_ABI := armeabi

# Locate OpenCL SDK root folder
INTELOCLSDKROOT=/cygdrive/c/android


# Setting LOCAL_CFLAGS with -I is not good in comparison to LOCAL_C_INCLUDES
# according to NDK documentation, but this only variant that works correctly
LOCAL_CFLAGS += -IC:/android/khronos_headers/khronos_headers/CL1.2

LOCAL_MODULE    := step
LOCAL_SRC_FILES := step.cpp
LOCAL_LDFLAGS += -llog -ljnigraphics -L$(LOCAL_PATH) -lOpenCL


include $(BUILD_SHARED_LIBRARY)
