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

#include "dlt-jni.h"

char *loadedDltFile = NULL;

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_MainActivity_setDltServerFilter(
        JNIEnv *env, jobject instance, jstring filter_in) {

    const char *filter = (*env)->GetStringUTFChars(env, filter_in, NULL);

    LOGI("set filter to jni: %s\n", filter);

    dlt_filter_init(&(dltfilter), 0);
    if (filter)
    {
        if (dlt_filter_load(&(dltfilter), filter, 0) < DLT_RETURN_OK)
        {
            return;
        }
    }

    if(loadedDltFile != NULL) {
        Java_com_zeerd_dltviewer_MainActivity_loadDltFile(env, instance, loadedDltFile);
    }
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_FilterActivity_setDltServerFilter(
        JNIEnv *env, jobject instance, jstring filter_in) {
    Java_com_zeerd_dltviewer_MainActivity_setDltServerFilter(env, instance, filter_in);
}
