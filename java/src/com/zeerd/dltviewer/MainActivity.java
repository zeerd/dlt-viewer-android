/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zeerd.dltviewer;

//import android.support.annotation.Keep;
import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import com.zeerd.dltviewer.R;
import android.os.Build;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.view.View;
import android.widget.CheckBox;

public class MainActivity extends Activity {

    int type = 2; /* 0=header, 1=payload, 2=msg */
    String sHeader= "";
    ScrollView scrollView;
    CheckBox checkBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String string = bundle.getString("msg");
                //final TextView myTextView = (TextView)findViewById(R.id.hellojniMsg);
                // myTextView.setText(string);
                Log.i(TAG, "" + string + ":" + type);
                if(string.equals("header") && type == 2) {
                    type = 0;
                }
                else if(string.equals("payload") && type == 2) {
                    type = 1;
                }
                else {
                    if(type == 0) {
                        sHeader = string;
                    }
                    else if(type == 1) {
                        String[] splited = sHeader.split("\\s+");

                        TableLayout table = (TableLayout)MainActivity.this.findViewById(R.id.log_table);

                        // Inflate your row "template" and fill out the fields.
                        TableRow row = (TableRow)LayoutInflater.from(MainActivity.this).inflate(R.layout.log_row, null);
                        ((TextView)row.findViewById(R.id.log_timestamp)).setText(splited[2]);
                        ((TextView)row.findViewById(R.id.log_ecuid)).setText(splited[4]);
                        ((TextView)row.findViewById(R.id.log_apid)).setText(splited[5]);
                        ((TextView)row.findViewById(R.id.log_ctid)).setText(splited[6]);
                        ((TextView)row.findViewById(R.id.log_payload)).setText(string);
                        table.addView(row);

                        table.requestLayout();     // Not sure if this is needed.

                        if(checkBox.isChecked()) {
                            scrollView.post(new Runnable() {            
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);              
                                }
                            });
                        }
                    }
                    type = 2;
                }

            }
        };
        staticHandler = handler;

        setContentView(R.layout.activity_main);
        scrollView = (ScrollView)MainActivity.this.findViewById(R.id.log_scroll);

        EditText ip = (EditText)MainActivity.this.findViewById(R.id.ip);
        ip.setText("192.168.42.210");
        SetDltServerIp(ip.getText().toString());

        checkBox = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_scroll);
        checkBox.setChecked(true);

    }
    @Override
    public void onResume() {
        super.onResume();
        //((TextView)findViewById(R.id.hellojniMsg)).setText(stringFromJNI());
        startLogs();

        TableLayout table = (TableLayout)MainActivity.this.findViewById(R.id.log_table);

        // Inflate your row "template" and fill out the fields.
        TableRow row = (TableRow)LayoutInflater.from(MainActivity.this).inflate(R.layout.log_row, null);
        ((TextView)row.findViewById(R.id.log_timestamp)).setText("Timestamp");
        ((TextView)row.findViewById(R.id.log_ecuid)).setText("Ecuid");
        ((TextView)row.findViewById(R.id.log_apid)).setText("Apid");
        ((TextView)row.findViewById(R.id.log_ctid)).setText("Ctid");
        ((TextView)row.findViewById(R.id.log_payload)).setText("Payload");
        table.addView(row);

        table.requestLayout();     // Not sure if this is needed.

        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause () {
        super.onPause();
        StopLogs();

        Log.i(TAG, "onPause");
    }   

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native  String stringFromJNI();
    public native void startLogs();
    public native void StopLogs();
    public native void SetDltServerIp(String ip);

    private static final String TAG = "DLT-Viewer";
    private static Handler staticHandler;

    /*
     * Print out status to logcat
     */
    //@Keep
    private void updateStatus(String txt) {
        if (txt.toLowerCase().contains("error")) {
            Log.e(TAG, "Native Err: " + txt);
        } else {
            Log.i(TAG, "Native Msg: " + txt);
        }
        Message msg = staticHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("msg", txt);
        msg.setData(bundle);
        staticHandler.sendMessage(msg);
    }

    /*
     * Return OS build version: a static function
     */
    //@Keep
    static public String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    /*
     * Return Java memory info
     */
    //@Keep
    public long getRuntimeMemorySize() {
        return Runtime.getRuntime().freeMemory();
    }

}
