package com.zeerd.dltviewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class CustomExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultUEH;
    private Activity mActivity;

    /*
     * if any of the parameters is null, the respective functionality
     * will not be used
     */
    CustomExceptionHandler(Activity activity) {
        this.mActivity = activity;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
        SharedPreferences prefs = this.mActivity.getSharedPreferences(
                "com.zeerd.dltviewer", Context.MODE_PRIVATE);
        boolean dbg = prefs.getBoolean("com.zeerd.dltviewer.debug", true);

        if(dbg) {
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            String stacktrace = result.toString();
            printWriter.close();

            writeToFile(stacktrace);
        }

        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String stacktrace) {
        String filename = new GenDltFilename("dlt", "stacktrace").getPath();

        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
