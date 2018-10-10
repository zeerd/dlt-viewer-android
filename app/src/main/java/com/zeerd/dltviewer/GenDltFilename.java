/*
 * @licence app begin@
 *
 * Copyright (C) 2018, Charles Chan <emneg#zeerd.com>
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License (MPL), v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * @licence end@
 */

package com.zeerd.dltviewer;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class GenDltFilename {

    private String path;

    GenDltFilename(String base, String ext) {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyyMMddHHmmss");

        File download = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "/dlt");

        String dd = mdformat.format(calendar.getTime());

        if(!download.exists()) {
            download.mkdirs();
        }

        path = download.getPath() + "/" + base + "-" + dd + "." + ext;
    }

    public String getPath() {
        return path;
    }
}
