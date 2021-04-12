#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <array>
#include <string>
#include <map>
#include <iostream>
#include <sstream>

#include <FaceDetector.h>
#include <CStruct.h>
#include <Struct.h>
#include <Struct_cv.h>

#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <seeta/CFaceDetector.h>

#define TAG "SeetaAiCS" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_DEBUG , TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)

static seeta::FaceDetector *FD;

extern "C"
JNIEXPORT void JNICALL
Java_com_seeta_sdk_FaceDetector_construct(JNIEnv *env, jobject faceDetector,
                                          jobject seetaModelSetting) {

    /* Get objarg's class - objarg is the one we pass from
       Java */
    jclass cls = (*env).GetObjectClass(seetaModelSetting);


    jfieldID fidId = (*env).GetFieldID(cls, "id", "I");
    if (fidId == NULL) {
        LOGE("get fidId id error");
        return; /* failed to find the field */
    }
//    LOGD("seetaModelSetting, GetFieldID:> %s", fidId);
    jint iVal = (*env).GetIntField(seetaModelSetting, fidId);
    LOGD("seetaModelSetting, GetIntField:> %i", iVal);
    /* For accessing primitive types from class use
           following field descriptors

           +---+---------+
           | Z | boolean |
           | B | byte    |
           | C | char    |
           | S | short   |
           | I | int     |
           | J | long    |
           | F | float   |
           | D | double  |
           +-------------+
    */
    jfieldID fidModelId = (*env).GetFieldID(cls, "model", "[Ljava/lang/String;");
    if (fidModelId == NULL) {
        LOGE("get fidModelId id error");
        return; /* failed to find the field */
    }
    // Get the object field, returns JObject (because Array is instance of Object)
    jobject modelArray = (*env).GetObjectField(seetaModelSetting, fidModelId);
    jobjectArray *objectArray = reinterpret_cast<jobjectArray *>(&modelArray);
    jobject representation = (jobject) env->GetObjectArrayElement(*objectArray, 0);

    int stringCount = env->GetArrayLength(*objectArray);

//    for (int i = 0; i < stringCount; i++) {
    jstring markerModelFile_ = (jstring) (env->GetObjectArrayElement(*objectArray, 0));
    const char *detectModelFile = env->GetStringUTFChars(markerModelFile_, 0);
    LOGD("*detectModelFile:> %s", detectModelFile);
    // Don't forget to call `ReleaseStringUTFChars` when you're done.
//    }

    // add image filter
    seeta::ModelSetting::Device device = seeta::ModelSetting::AUTO;

    int id = 0;
    seeta::ModelSetting FD_model(detectModelFile, device, id);

    FD = new seeta::FaceDetector(FD_model);
//    FD->set(seeta::FaceDetector::Property::PROPERTY_NUMBER_THREADS, 4);
    FD->set(seeta::FaceDetector::PROPERTY_THRESHOLD, 0.65f);
    FD->set(seeta::FaceDetector::PROPERTY_MIN_FACE_SIZE, 80.0f);

    LOGD("Load Seeta Face Detector Model Successfully.");

    int res = EXIT_SUCCESS;
    env->ReleaseStringUTFChars(markerModelFile_, detectModelFile);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_seeta_sdk_FaceDetector_dispose(JNIEnv *env, jobject faceDetector) {
    delete FD;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_seeta_sdk_FaceDetector_Detect(JNIEnv *env, jobject faceDetector,
                                       jobject seetaImageData) {

//    LOGD("width:> %i", seetaImageData_c.width);
//    LOGD("height:> %i",seetaImageData_c.height);
//    LOGD("channels:> %i",seetaImageData_c.channels);

    jobjectArray ret;

    jclass seetaImgDataCls = (*env).GetObjectClass(seetaImageData);

    jfieldID fidWidthId = (*env).GetFieldID(seetaImgDataCls, "width", "I");
//    if (fidWidthId == NULL) {
//        LOGE("get fidWidthId id error");
//        return (jobjectArray) ret; /* failed to find the field */
//    }
    jint widthValue = (*env).GetIntField(seetaImageData, fidWidthId);

    jfieldID fidHeightId = (*env).GetFieldID(seetaImgDataCls, "height", "I");
//    if (fidHeightId == NULL) {
//        LOGE("get fidHeightId id error");
//        return (jobjectArray) ret; /* failed to find the field */
//    }
    jint heightValue = (*env).GetIntField(seetaImageData, fidHeightId);

    jfieldID fidChannelId = (*env).GetFieldID(seetaImgDataCls, "channels", "I");
//    if (fidChannelId == NULL) {
//        LOGE("get fidChannelId id error");
//        return (jobjectArray) ret; /* failed to find the field */
//    }
    jint channelValue = (*env).GetIntField(seetaImageData, fidChannelId);

    jfieldID fidDataId = (*env).GetFieldID(seetaImgDataCls, "data", "[B");
//    if (fidDataId == NULL) {
//        LOGE("get fidDataId id error");
//        return (jobjectArray) ret;
//    }
    jobject dataArray = (*env).GetObjectField(seetaImageData, fidDataId);
    jbyteArray *dataByteArray = reinterpret_cast<jbyteArray *>(&dataArray);
//    int len = env->GetArrayLength(*dataByteArray);
//    unsigned char *buf = new unsigned char[len];
//    env->GetByteArrayRegion(*dataByteArray, 0, len, reinterpret_cast<jbyte *>(buf));

    unsigned char* szStr= NULL;
    szStr = (unsigned char*)(*env).GetByteArrayElements(*dataByteArray, NULL);


    SeetaImageData simg;
    simg.width = widthValue;
    simg.height = heightValue;
    simg.channels = channelValue;
//    simg.data = buf;
    simg.data = szStr;

    //然后去用szStr吧，就是对jbyteArray szLics的使用
//    env->ReleaseByteArrayElements(*dataByteArray, reinterpret_cast<jbyte *>(szStr), 0);

    SeetaFaceInfoArray faces = FD->detect(simg);
//    LOGD("so. seetaImageData, faces.size:> %i", faces.size);

    jclass seetaRectObjectClass = (env)->FindClass("com/seeta/sdk/SeetaRect");
//    jobjectArray args = (env)->NewObjectArray(faces.size, seetaRectObjectClass, 0);
    jobjectArray args = (env)->NewObjectArray(faces.size, seetaRectObjectClass, 0);

    // 获取类的构造函数，记住这里是调用无参的构造函数
    jmethodID cid = (*env).GetMethodID(seetaRectObjectClass, "<init>", "()V");

    for (int i = 0; i < faces.size; i++) {
        jfieldID x = (env)->GetFieldID(seetaRectObjectClass, "x", "I");
        jfieldID y = (env)->GetFieldID(seetaRectObjectClass, "y", "I");
        jfieldID width = (env)->GetFieldID(seetaRectObjectClass, "width", "I");
        jfieldID height = (env)->GetFieldID(seetaRectObjectClass, "height", "I");

        // 创建一个新的对象
        jobject jseetaRect = env->NewObject(seetaRectObjectClass, cid);

        auto &face = faces.data[i];
        auto &pos = face.pos;

//        SeetaRect rect = face.pos;

        env->SetIntField(jseetaRect, x, pos.x);
        env->SetIntField(jseetaRect, y, pos.y);
        env->SetIntField(jseetaRect, width, pos.width);
        env->SetIntField(jseetaRect, height, pos.height);

        env->SetObjectArrayElement(args, i, jseetaRect);
    }

//    env->DeleteLocalRef(seetaRectObjectClass);
    return (jobjectArray) args;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_seeta_sdk_FaceDetector_set(JNIEnv *env, jobject faceDetector, jobject property,
                                    jdouble value) {

    jclass seetaPropertyCls = (*env).GetObjectClass(property);

    seeta::FaceDetector::Property *cProperty = reinterpret_cast<seeta::FaceDetector::Property *>(property);
    FD->set(*cProperty, value);
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_seeta_sdk_FaceDetector_get(JNIEnv *env, jobject faceDetector, jobject property) {

    seeta::FaceDetector::Property *cProperty = reinterpret_cast<seeta::FaceDetector::Property *>(property);
    return (jdouble) FD->get(*cProperty);
}
