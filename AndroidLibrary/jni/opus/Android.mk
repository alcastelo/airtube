LOCAL_PATH := $(call my-dir)/opus-1.1

include $(CLEAR_VARS)

OPUS_PATH	:= opus-1.1
LOCAL_MODULE	:= libopus
#LOCAL_CFLAGS	:= -Drestrict='' -D__EMX__
LOCAL_CFLAGS	:= -DOPUS_BUILD -DFIXED_POINT -DDISABLE_FLOAT_API -DUSE_ALLOCA \
		  -DHAVE_LRINT -DHAVE_LRINTF -O3 -fno-math-errno
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include $(LOCAL_PATH)/celt $(LOCAL_PATH)/silk \
		    $(LOCAL_PATH)/silk/fixed
LOCAL_LDLIBS	:= -llog

include $(LOCAL_PATH)/opus_sources.mk
include $(LOCAL_PATH)/celt_sources.mk
include $(LOCAL_PATH)/silk_sources.mk

#$(CELT_SOURCES_ARM) $(CELT_SOURCES_ARM_ASM)
LOCAL_SRC_FILES := $(OPUS_SOURCES) $(CELT_SOURCES) $(SILK_SOURCES) $(SILK_SOURCES_FIXED)
LOCAL_SRC_FILES += ../opus_jni_wrapper.c

include $(BUILD_SHARED_LIBRARY)
