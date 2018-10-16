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

#ifndef DLT_VIEWER_ANDROID_DLT_JNI_H
#define DLT_VIEWER_ANDROID_DLT_JNI_H

#include <string.h>

#include <jni.h>
#include <android/log.h>

#include "dlt_client.h"

#define DLT_RECEIVE_TEXTBUFSIZE 10024  /* Size of buffer for text output */

#define DLT_RECEIVE_ECU_ID "RECV"

#define UNUSED(x) (void)(x)

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
    int      running;
    jmethodID statusId;
    JNIEnv *env;
} LogContext;

extern char *loadedDltFile;
extern char ecuid[4+1];
extern int vflag;

extern LogContext g_ctx;

extern DltFilter dltfilter;
extern DltClient dltclient;

extern void send_message_to_java(LogContext *pctx, DltMessage *message);

extern JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_loadDltFile(JNIEnv *env, jobject instance, jstring file);

#endif //DLT_VIEWER_ANDROID_DLT_JNI_H
