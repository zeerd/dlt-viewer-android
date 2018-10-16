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
#include "dlt-jni.h"

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_loadDltFile(JNIEnv *env, jobject instance, jstring file) {

    UNUSED(instance);

    DltFile dlt_file;
    dlt_file_init(&dlt_file, vflag);

    const char *f = (*env)->GetStringUTFChars(env, file, NULL);
    LOGI("loading dlt file : %s.\n", f);

    if (dlt_file_open(&dlt_file, f, vflag) >= DLT_RETURN_OK) {
        while (dlt_file_read(&dlt_file, vflag) >= DLT_RETURN_OK) {
        }
    }
    else {
        LOGE("load dlt file failed : %s.\n", f);
    }

    int num;
    for (num = 0; num <= dlt_file.counter-1 ;num++) {
        dlt_file_message(&dlt_file, num, vflag);

        if(dlt_message_filter_check(&(dlt_file.msg),&(dltfilter),0) == DLT_RETURN_TRUE) {

            send_message_to_java(&g_ctx, &(dlt_file.msg));
        }
    }

    if(loadedDltFile != NULL) {
        free(loadedDltFile);
    }
    loadedDltFile = strdup(f);

    dlt_file_free(&dlt_file,vflag);
}
