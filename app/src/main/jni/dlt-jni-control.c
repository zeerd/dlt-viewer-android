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

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_ControlActivity_setDefaultLevel(JNIEnv *env, jobject instance, jint level) {

    UNUSED(env);
    UNUSED(instance);

    dlt_client_send_default_log_level(&dltclient, (uint8_t)level);
    LOGI("set default log level to %d.\n", (int)level);
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_ControlActivity_setAllLevel(JNIEnv *env, jobject instance, jint level) {

    UNUSED(env);
    UNUSED(instance);

    dlt_client_send_all_log_level(&dltclient, (uint8_t)level);
    LOGI("set all log level to %d.\n", (int)level);
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_ControlActivity_setLevel(JNIEnv *env, jobject instance, jstring apid, jstring ctid, jint level) {

    UNUSED(instance);

    const char *a = (*env)->GetStringUTFChars(env, apid, NULL);
    const char *c = (*env)->GetStringUTFChars(env, ctid, NULL);
    dlt_client_send_log_level(&dltclient, (char *) a, (char *) c, (uint8_t)level);
    LOGI("set [%s:%s] log level to %d.\n", a, c, (int)level);
}

static void hexAsciiToBinary (const char *ptr, uint8_t *binary, uint32_t *size)
{
    char ch = *ptr;
    uint32_t pos = 0;
    binary[pos] = 0;
    int first = 1;
    int found;

    for(;;) {
        if(ch == 0) {
            *size = pos;
            return;
        }
        found = 0;
        if (ch >= '0' && ch <= '9') {
            binary[pos] = (uint8_t) ((binary[pos] << 4) + (ch - '0'));
            found = 1;
        }
        else if (ch >= 'A' && ch <= 'F') {
            binary[pos] = (uint8_t) ((binary[pos] << 4) + (ch - 'A' + 10));
            found = 1;
        }
        else if (ch >= 'a' && ch <= 'f') {
            binary[pos] = (uint8_t) ((binary[pos] << 4) + (ch - 'a' + 10));
            found = 1;
        }
        if(found) {
            if(first) {
                first = 0;
            }
            else {
                first = 1;
                pos++;
                if(pos>=*size)
                    return;
                binary[pos]=0;
            }
        }
        ch = *(++ptr);
    }
}

JNIEXPORT void JNICALL
Java_com_zeerd_dltviewer_ControlActivity_sendInject(JNIEnv *env, jobject instance, jstring apid, jstring ctid, jint sid, jstring msg, jint hex) {

    UNUSED(instance);

    const char *a = (*env)->GetStringUTFChars(env, apid, NULL);
    const char *c = (*env)->GetStringUTFChars(env, ctid, NULL);
    uint32_t s = (uint32_t)sid;
    const char *m = (*env)->GetStringUTFChars(env, msg, NULL);

    if(hex) {
        uint8_t buffer[1024];
        uint32_t size = 1024;
        hexAsciiToBinary(m, buffer, &size);
        dlt_client_send_inject_msg(&dltclient, (char *)a, (char *)c, s, buffer, size);
    }
    else {
        dlt_client_send_inject_msg(&dltclient, (char *)a, (char *)c, s, (uint8_t*)m, (uint32_t)strlen(m));
    }

    LOGI("send inject message to [%s:%s:%d] : %s.\n", a, c, s, m);
}
