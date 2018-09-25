LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := dlt-jnicallback
#LOCAL_STATIC_LIBRARIES := <module_name>
LOCAL_C_INCLUDES := 
LOCAL_SRC_FILES := dlt-jnicallback.c dlt_common.c dlt_client.c

LOCAL_LDLIBS   = -lz -lm -llog
LOCAL_CFLAGS   = -Wall -g -Wno-incompatible-pointer-types

include $(BUILD_SHARED_LIBRARY)