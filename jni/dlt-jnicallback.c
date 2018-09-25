/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <inttypes.h>
#include <pthread.h>
#include <jni.h>
#include <android/log.h>
#include <assert.h>

#include <ctype.h>      /* for isprint() */
#include <stdlib.h>     /* for atoi() */
#include <sys/stat.h>   /* for S_IRUSR, S_IWUSR, S_IRGRP, S_IROTH */
#include <fcntl.h>      /* for open() */
#include <sys/uio.h>    /* for writev() */
#include <errno.h>
#include <string.h>
#include <glob.h>
#include <syslog.h>
#include <linux/limits.h> /* for PATH_MAX */
#include <inttypes.h>

#include "dlt_client.h"

#define DLT_RECEIVE_TEXTBUFSIZE 10024  /* Size of buffer for text output */

#define DLT_RECEIVE_ECU_ID "RECV"

// Android log function wrappers
static const char* kTAG = "dlt-jniCallback";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

// processing callback to handler class
typedef struct log_context {
    JavaVM  *javaVM;
    jclass   jniHelperClz;
    jobject  jniHelperObj;
    jclass   mainActivityClz;
    jobject  mainActivityObj;
    pthread_mutex_t  lock;
    int      done;
    jmethodID statusId;
    JNIEnv *env;
} LogContext;
LogContext g_ctx;

static char *default_ip = "192.168.42.210";
static char *ip = NULL;

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   hello-jniCallback/app/src/main/java/com/example/hellojnicallback/MainActivity.java
 */
JNIEXPORT jstring JNICALL
Java_com_zeerd_dltviewer_MainActivity_stringFromJNI( JNIEnv* env, jobject thiz )
{
#if defined(__arm__)
    #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
    #else
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a (hard-float)"
      #else
        #define ABI "armeabi-v7a"
      #endif
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif
    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI " ABI ".");
}

/*
 *  A helper function to show how to call
 *     java static functions JniHelper::getBuildVersion()
 *     java non-static function JniHelper::getRuntimeMemorySize()
 *  The trivial implementation for these functions are inside file
 *     JniHelper.java
 */
void queryRuntimeInfo(JNIEnv *env, jobject instance) {
    // Find out which OS we are running on. It does not matter for this app
    // just to demo how to call static functions.
    // Our java JniHelper class id and instance are initialized when this
    // shared lib got loaded, we just directly use them
    //    static function does not need instance, so we just need to feed
    //    class and method id to JNI
    jmethodID versionFunc = (*env)->GetStaticMethodID(
            env, g_ctx.jniHelperClz,
            "getBuildVersion", "()Ljava/lang/String;");
    if (!versionFunc) {
        LOGE("Failed to retrieve getBuildVersion() methodID @ line %d",
             __LINE__);
        return;
    }
    jstring buildVersion = (*env)->CallStaticObjectMethod(env,
                                                          g_ctx.jniHelperClz, versionFunc);
    const char *version = (*env)->GetStringUTFChars(env, buildVersion, NULL);
    if (!version) {
        LOGE("Unable to get version string @ line %d", __LINE__);
        return;
    }
    LOGI("Android Version - %s", version);
    (*env)->ReleaseStringUTFChars(env, buildVersion, version);

    // we are called from JNI_OnLoad, so got to release LocalRef to avoid leaking
    (*env)->DeleteLocalRef(env, buildVersion);

    // Query available memory size from a non-static public function
    // we need use an instance of JniHelper class to call JNI
    jmethodID memFunc = (*env)->GetMethodID(env, g_ctx.jniHelperClz,
                                            "getRuntimeMemorySize", "()J");
    if (!memFunc) {
        LOGE("Failed to retrieve getRuntimeMemorySize() methodID @ line %d",
             __LINE__);
        return;
    }
    jlong result = (*env)->CallLongMethod(env, instance, memFunc);
    LOGI("Runtime free memory size: %" PRId64, result);
    (void)result;  // silence the compiler warning
}

/*
 * processing one time initialization:
 *     Cache the javaVM into our context
 *     Find class ID for JniHelper
 *     Create an instance of JniHelper
 *     Make global reference since we are using them from a native thread
 * Note:
 *     All resources allocated here are never released by application
 *     we rely on system to free all global refs when it goes away;
 *     the pairing function JNI_OnUnload() never gets called at all.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    memset(&g_ctx, 0, sizeof(g_ctx));

    g_ctx.javaVM = vm;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    jclass  clz = (*env)->FindClass(env,
                                    "com/zeerd/dltviewer/MainActivity");
    g_ctx.jniHelperClz = (*env)->NewGlobalRef(env, clz);

    jmethodID  jniHelperCtor = (*env)->GetMethodID(env, g_ctx.jniHelperClz,
                                                   "<init>", "()V");
    jobject    handler = (*env)->NewObject(env, g_ctx.jniHelperClz,
                                           jniHelperCtor);
    g_ctx.jniHelperObj = (*env)->NewGlobalRef(env, handler);
    queryRuntimeInfo(env, g_ctx.jniHelperObj);

    g_ctx.done = 0;
    g_ctx.mainActivityObj = NULL;

    ip = default_ip;
    return  JNI_VERSION_1_6;
}

/*
 * A helper function to wrap java JniHelper::updateStatus(String msg)
 * JNI allow us to call this function via an instance even it is
 * private function.
 */
void   sendJavaMsg(JNIEnv *env, jobject instance,
                   jmethodID func,const char* msg) {

    // LOGI("%s ",msg);

    jstring javaMsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallVoidMethod(env, instance, func, javaMsg);
    (*env)->DeleteLocalRef(env, javaMsg);
}

int dlt_receive_message_callback(DltMessage *message, void *data)
{
    LogContext *pctx = (LogContext*) data;
    static char text[DLT_RECEIVE_TEXTBUFSIZE];

    if ((message==0) || (data==0))
    {
        return -1;
    }

    /* prepare storage header */
    if (DLT_IS_HTYP_WEID(message->standardheader->htyp))
    {
        dlt_set_storageheader(message->storageheader,message->headerextra.ecu);
    }
    else
    {
        dlt_set_storageheader(message->storageheader,"AECU");
    }

    dlt_message_header(message,text,DLT_RECEIVE_TEXTBUFSIZE, 0);
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, "header");
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, text);

    dlt_message_payload(message,text,DLT_RECEIVE_TEXTBUFSIZE,DLT_OUTPUT_ASCII,0);
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, "payload");
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, text);

    return 0;
}

/*
 * Main working thread function. From a pthread,
 *     calling back to MainActivity::updateStatus(String msg) for msg
 */
void*  UpdateLogs(void* context) {
    LogContext *pctx = (LogContext*) context;
    JavaVM *javaVM = pctx->javaVM;

    DltClient      dltclient;

    LOGI("enter update Logs from jni\n");

    jint res = (*javaVM)->GetEnv(javaVM, (void**)&(pctx->env), JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM)->AttachCurrentThread(javaVM, &(pctx->env), NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }

    pctx->statusId = (*(pctx->env))->GetMethodID((pctx->env), pctx->jniHelperClz,
                                             "updateStatus",
                                             "(Ljava/lang/String;)V");
    sendJavaMsg((pctx->env), pctx->jniHelperObj, pctx->statusId,
                "LogerThread status: initializing...");

    /* Initialize DLT Client */
    dlt_client_init(&dltclient, 0);

    /* Register callback to be called when message was received */
    dlt_client_register_message_callback(dlt_receive_message_callback);

    dltclient.mode = DLT_CLIENT_MODE_TCP;
    dltclient.port = 3490;
    dlt_client_set_server_ip(&dltclient, ip);

    sendJavaMsg((pctx->env), pctx->jniHelperObj, pctx->statusId,
                "LogerThread status: start updating ...");

    /* Connect to TCP socket or open serial device */
    if (dlt_client_connect(&dltclient, 0) != DLT_RETURN_ERROR)
    {
        /* Dlt Client Main Loop */
        dlt_client_main_loop(&dltclient, pctx, 0);

        /* Dlt Client Cleanup */
        dlt_client_cleanup(&dltclient, 0);
    }
    else {
        LOGE("dlt connect failed from jni\n");
    }

    pctx->done = 0;

    sendJavaMsg((pctx->env), pctx->jniHelperObj, pctx->statusId,
                "LogerThread status: updating stopped");
    (*javaVM)->DetachCurrentThread(javaVM);
    return context;
}

/*
 * Interface to Java side to start receive dlt logs, caller is from onResume()
 */
JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_startLogs(JNIEnv *env, jobject instance) {
    pthread_t       threadInfo_;
    pthread_attr_t  threadAttr_;

    pthread_attr_init(&threadAttr_);
    pthread_attr_setdetachstate(&threadAttr_, PTHREAD_CREATE_DETACHED);

    pthread_mutex_init(&g_ctx.lock, NULL);

    jclass clz = (*env)->GetObjectClass(env, instance);
    g_ctx.mainActivityClz = (*env)->NewGlobalRef(env, clz);
    g_ctx.mainActivityObj = (*env)->NewGlobalRef(env, instance);

    int result  = pthread_create( &threadInfo_, &threadAttr_, UpdateLogs, &g_ctx);
    assert(result == 0);

    pthread_attr_destroy(&threadAttr_);

    (void)result;
    LOGI("run start Logs from jni : %d\n", result);
}

/*
 * Interface to Java side to stop receive dlt logs:
 *    we need to hold and make sure our native thread has finished before return
 *    for a clean shutdown. The caller is from onPause
 */
JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_StopLogs(JNIEnv *env, jobject instance) {
    pthread_mutex_lock(&g_ctx.lock);
    g_ctx.done = 1;
    pthread_mutex_unlock(&g_ctx.lock);

    // waiting for updating thread to flip the done flag
    struct timespec sleepTime;
    memset(&sleepTime, 0, sizeof(sleepTime));
    sleepTime.tv_nsec = 100000000;
    while (g_ctx.done) {
        nanosleep(&sleepTime, NULL);
    }

    // release object we allocated from StartLogs() function
    (*env)->DeleteGlobalRef(env, g_ctx.mainActivityClz);
    (*env)->DeleteGlobalRef(env, g_ctx.mainActivityObj);
    g_ctx.mainActivityObj = NULL;
    g_ctx.mainActivityClz = NULL;

    pthread_mutex_destroy(&g_ctx.lock);

    LOGI("run stop Logs from jni\n");
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_SetDltServerIp(
                    JNIEnv *env, jobject instance, jstring ip_in) {

    if(ip != default_ip && ip != NULL) {
        free(ip);
    }

    ip = strdup((*env)->GetStringUTFChars(env, ip_in, NULL));

    LOGI("set ip to jni: %s\n", ip);
}