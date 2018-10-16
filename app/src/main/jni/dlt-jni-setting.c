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
Java_com_zeerd_dltviewer_SettingActivity_setEcuID(
        JNIEnv *env, jobject instance, jstring ecu_in) {

    UNUSED(instance);

    const char *e = (*env)->GetStringUTFChars(env, ecu_in, NULL);
    strncpy(ecuid, e, 4);

    LOGI("set ECU ID to %s.\n", ecuid);
}
