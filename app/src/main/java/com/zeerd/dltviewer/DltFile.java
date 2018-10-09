package com.zeerd.dltviewer;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DltFile {

    private String path;

    DltFile(String base, String ext) {

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
