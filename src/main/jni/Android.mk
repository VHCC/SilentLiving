LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ts-prebuilt
LOCAL_SRC_FILES := $(LOCAL_PATH)/jniLibs/$(TARGET_ARCH_ABI)/libTenniS.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := seetaauthorize-prebuilt
LOCAL_SRC_FILES := $(LOCAL_PATH)/jniLibs/$(TARGET_ARCH_ABI)/libSeetaAuthorize.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := seetafacedetector600-prebuilt
LOCAL_SRC_FILES := $(LOCAL_PATH)/jniLibs/$(TARGET_ARCH_ABI)/libSeetaFaceDetector600.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv3-prebuilt
LOCAL_SRC_FILES := $(LOCAL_PATH)/jniLibs/$(TARGET_ARCH_ABI)/libopencv_java3.so
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := SeetaAiCSFaceDetector

LOCAL_SRC_FILES := facedetector_java.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/seeta/
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/seeta/Common/
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/opencv2/

LOCAL_LDFLAGS += -L$(LOCAL_PATH)/lib -fuse-ld=bfd.exe

LOCAL_LDLIBS += -llog -lz

LOCAL_CFLAGS += -mfpu=neon-vfpv4 -funsafe-math-optimizations -ftree-vectorize  -ffast-math

## 匯入logcat日誌庫
# LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

LOCAL_SHARED_LIBRARIES += ts-prebuilt
LOCAL_SHARED_LIBRARIES += seetaauthorize-prebuilt
LOCAL_SHARED_LIBRARIES += seetafacedetector600-prebuilt
LOCAL_SHARED_LIBRARIES += opencv3-prebuilt


include $(BUILD_SHARED_LIBRARY)