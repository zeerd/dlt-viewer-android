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

#include <malloc.h>
#include <pthread.h>
#include <assert.h>
#include "dlt-jni.h"

static int running = 0;

/*
 * Main working thread function. From a pthread,
 *     calling back to MainActivity::updateStatus(String msg) for msg
 */
static void*  loadDltFileThread(void* context) {

    JavaVM *javaVM = g_ctx.javaVM;

    LOGI("enter load dlt file from jni\n");

    jint res = (*javaVM)->GetEnv(javaVM, (void**)&(g_ctx.env), JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM)->AttachCurrentThread(javaVM, &(g_ctx.env), NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }

    running = 1;

    DltFile dlt_file;
    dlt_file_init(&dlt_file, vflag);

    if (dlt_file_open(&dlt_file, loadedDltFile, vflag) >= DLT_RETURN_OK) {
        while (dlt_file_read(&dlt_file, vflag) >= DLT_RETURN_OK  && running) {
        }
    }
    else {
        LOGE("load dlt file failed : %s.\n", loadedDltFile);
    }

    int num;
    for (num = 0; (num <= dlt_file.counter-1 && running);num++) {
        dlt_file_message(&dlt_file, num, vflag);

        if(dlt_message_filter_check(&(dlt_file.msg),&(dltfilter),0) == DLT_RETURN_TRUE) {

            send_message_to_java(&g_ctx, &(dlt_file.msg));
        }
    }

    dlt_file_free(&dlt_file,vflag);

    send_dlt_load_status_to_java(&g_ctx, loadedDltFile);

    (*javaVM)->DetachCurrentThread(javaVM);

    LOGI("quit load dlt file from jni\n");
    running = 0;
    return context;
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_loadDltFile(JNIEnv *env, jobject instance, jstring file) {

    UNUSED(instance);

    const char *f = (*env)->GetStringUTFChars(env, file, NULL);
    LOGI("loading dlt file : %s.\n", f);

    if(loadedDltFile != NULL) {
        free(loadedDltFile);
    }
    loadedDltFile = strdup(f);

    pthread_attr_t  threadAttr_;

    pthread_attr_init(&threadAttr_);
    pthread_attr_setdetachstate(&threadAttr_, PTHREAD_CREATE_DETACHED);

    pthread_t threadInfo_;
    int result  = pthread_create( &threadInfo_, &threadAttr_, loadDltFileThread, NULL);
    assert(result == 0);

    pthread_attr_destroy(&threadAttr_);

    (void)result;
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_stoploadingDltFile(JNIEnv *env, jobject instance) {

    UNUSED(env);
    UNUSED(instance);

    LOGI("stop loading dlt file.\n");
    running = 0;
}
