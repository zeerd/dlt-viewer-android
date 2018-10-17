/*
 * @licence app begin@
 *
 * Copyright (C) 2018, Charles Chan <charles#zeerd.com>
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License (MPL), v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * @licence end@
 */

#include <inttypes.h>
#include <pthread.h>
#include <assert.h>

#include <ctype.h>      /* for isprint() */
#include <stdlib.h>     /* for atoi() */
#include <sys/stat.h>   /* for S_IRUSR, S_IWUSR, S_IRGRP, S_IROTH */
#include <fcntl.h>      /* for open() */
#include <sys/uio.h>    /* for writev() */
#include <errno.h>
#include <glob.h>
#include <syslog.h>
#include <linux/limits.h> /* for PATH_MAX */
#include <inttypes.h>

#include "dlt-jni.h"

LogContext g_ctx;

static char *default_ip = "192.168.42.210";
static char *ip = NULL;
static pthread_t threadInfo_;
static int ohandle = 0;

int vflag = 0;
char ecuid[4+1] = "RECV";
DltClient dltclient;
DltFilter dltfilter;

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
void thread_exit_handler(int sig)
{
    printf("this signal is %d \n", sig);
    pthread_exit(0);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {

    UNUSED(reserved);

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

    g_ctx.statusId = (*env)->GetMethodID(env, g_ctx.jniHelperClz,
                                                 "updateStatus",
                                                 "(Ljava/lang/String;)V");

    (*(g_ctx.javaVM))->GetEnv(g_ctx.javaVM, (void**)&(g_ctx.env), JNI_VERSION_1_6);

    g_ctx.running = 0;
    g_ctx.mainActivityObj = NULL;

    ip = default_ip;

    struct sigaction actions;
    memset(&actions, 0, sizeof(actions));
    sigemptyset(&actions.sa_mask);
    actions.sa_flags = 0;
    actions.sa_handler = thread_exit_handler;
    sigaction(SIGUSR1,&actions,NULL);

    pthread_mutex_init(&g_ctx.lock, NULL);

    LOGI("JNI_OnLoad() Done.");

    return  JNI_VERSION_1_6;
}

/*
 * A helper function to wrap java JniHelper::updateStatus(String msg)
 * JNI allow us to call this function via an instance even it is
 * private function.
 */
void   sendJavaMsg(JNIEnv *env, jobject instance,
                   jmethodID func,const char* msg) {

    // LOGI("%s %p %p %p",msg, env, instance, func);

    jstring javaMsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallVoidMethod(env, instance, func, javaMsg);
    (*env)->DeleteLocalRef(env, javaMsg);
}

void send_message_to_java(LogContext *pctx, DltMessage *message) {
    static char text[DLT_RECEIVE_TEXTBUFSIZE];

    dlt_message_header(message,text,DLT_RECEIVE_TEXTBUFSIZE, 0);
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, "header");
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, text);

    dlt_message_payload(message,text,DLT_RECEIVE_TEXTBUFSIZE,DLT_OUTPUT_ASCII,0);
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, "payload");
    sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, text);

    LOGI("send_message_to_java : %s\n", text);
}

int dlt_receive_message_callback(DltMessage *message, void *data)
{
    LogContext *pctx = (LogContext*) data;

    if ((message==0) || (data==0))
    {
        return -1;
    }

    /* prepare storage header */
    if (DLT_IS_HTYP_WEID(message->standardheader->htyp))
    {
        dlt_set_storageheader(message->storageheader, message->headerextra.ecu);
    }
    else
    {
        dlt_set_storageheader(message->storageheader, ecuid);
    }

    if(dlt_message_filter_check(message,&(dltfilter),0) == DLT_RETURN_TRUE) {

        send_message_to_java(pctx, message);
    }

    pthread_mutex_lock(&g_ctx.lock);
    if(ohandle > 0) {
        struct iovec iov[2];
        iov[0].iov_base = message->headerbuffer;
        iov[0].iov_len = (__kernel_size_t) message->headersize;
        iov[1].iov_base = message->databuffer;
        iov[1].iov_len = (__kernel_size_t) message->datasize;
        writev(ohandle, iov, 2);
    }
    pthread_mutex_unlock(&g_ctx.lock);

    return 0;
}

/*
 * Main working thread function. From a pthread,
 *     calling back to MainActivity::updateStatus(String msg) for msg
 */
void*  UpdateLogs(void* context) {
    LogContext *pctx = (LogContext*) context;
    JavaVM *javaVM = pctx->javaVM;

    LOGI("enter update Logs from jni\n");

    jint res = (*javaVM)->GetEnv(javaVM, (void**)&(pctx->env), JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM)->AttachCurrentThread(javaVM, &(pctx->env), NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }

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
     /* Workaround for issue: socket disconnect after about 20000 logs.
       No more data to be received. But don't know why. */
    while (pctx->running
        && dlt_client_connect(&dltclient, 0) != DLT_RETURN_ERROR
        )
    {
        /* Dlt Client Main Loop */
        dlt_client_main_loop(&dltclient, pctx, 0);

        /* Dlt Client Cleanup */
        dlt_client_cleanup(&dltclient, 0);
    }

    if(pctx->running) {
        LOGE("dlt connect failed from jni\n");
        sendJavaMsg(pctx->env, pctx->jniHelperObj, pctx->statusId, "$disconnect$");
    }

    sendJavaMsg((pctx->env), pctx->jniHelperObj, pctx->statusId,
                "LogerThread status: updating stopped");
    (*javaVM)->DetachCurrentThread(javaVM);

    LOGI("quit update Logs from jni\n");
    return context;
}

/*
 * Interface to Java side to start receive dlt logs, caller is from onResume()
 */
JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_startLogs(JNIEnv *env, jobject instance) {
    pthread_attr_t  threadAttr_;

    pthread_attr_init(&threadAttr_);
    pthread_attr_setdetachstate(&threadAttr_, PTHREAD_CREATE_DETACHED);

    jclass clz = (*env)->GetObjectClass(env, instance);
    g_ctx.mainActivityClz = (*env)->NewGlobalRef(env, clz);
    g_ctx.mainActivityObj = (*env)->NewGlobalRef(env, instance);

    int result  = pthread_create( &threadInfo_, &threadAttr_, UpdateLogs, &g_ctx);
    assert(result == 0);

    pthread_attr_destroy(&threadAttr_);

    (void)result;

    pthread_mutex_lock(&g_ctx.lock);
    g_ctx.running = 1;
    pthread_mutex_unlock(&g_ctx.lock);

    loadedDltFile = NULL;

    LOGI("run start Logs from jni : %d\n", result);
}

/*
 * Interface to Java side to stop receive dlt logs:
 *    we need to hold and make sure our native thread has finished before return
 *    for a clean shutdown. The caller is from onPause
 */
JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_stopLogs(JNIEnv *env, jobject instance) {

    UNUSED(instance);

    LOGI("run stop Logs from jni : IN\n");

    pthread_mutex_lock(&g_ctx.lock);
    dlt_client_cleanup(&dltclient, 0);

    // release object we allocated from StartLogs() function
    (*env)->DeleteGlobalRef(env, g_ctx.mainActivityClz);
    (*env)->DeleteGlobalRef(env, g_ctx.mainActivityObj);
    g_ctx.mainActivityObj = NULL;
    g_ctx.mainActivityClz = NULL;

    g_ctx.running = 0;

    pthread_mutex_unlock(&g_ctx.lock);
//    pthread_mutex_destroy(&g_ctx.lock);

    LOGI("run stop Logs from jni : OUT\n");
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_setDltServerIp(
                    JNIEnv *env, jobject instance, jstring ip_in) {

    UNUSED(instance);

    if(ip != default_ip && ip != NULL) {
        free(ip);
    }

    ip = strdup((*env)->GetStringUTFChars(env, ip_in, NULL));

    LOGI("set ip to jni: %s\n", ip);
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_startRecordLogs(JNIEnv *env, jobject instance, jstring file_in) {

    UNUSED(instance);

    const char *file = (*env)->GetStringUTFChars(env, file_in, NULL);
    LOGI("start to record logs : %s.\n", file);

    ohandle = open(file, O_WRONLY|O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if(ohandle <= 0) {
        LOGE("failed to open file : %s\n", file);
    }
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_stopRecordLogs(JNIEnv *env, jobject instance) {

    UNUSED(env);
    UNUSED(instance);

    pthread_mutex_lock(&g_ctx.lock);
    if (ohandle)
    {
        close(ohandle);
        ohandle = -1;
    }
    pthread_mutex_unlock(&g_ctx.lock);
    LOGI("stop to record logs.\n");
}

