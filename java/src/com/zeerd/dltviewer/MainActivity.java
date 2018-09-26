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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zeerd.dltviewer.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {

    int type = 2; /* 0=header, 1=payload, 2=msg */
    String sHeader= "";
    ScrollView scrollView;
    CheckBox checkBox;
    CheckBox checkBoxConn;
    EditText ip;


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
                else if(string.equals("$disconnect$")) {
                    checkBoxConn.post(new Runnable() {            
                        @Override
                        public void run() {
                            checkBoxConn.setChecked(false);     
                            Toast.makeText(getBaseContext(), 
                                "Disconnected from daemon.",
                                                Toast.LENGTH_LONG).show();
                        }
                    }); 
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
                        ((TextView)row.findViewById(R.id.log_subtype)).setText(splited[8]);
                        ((TextView)row.findViewById(R.id.log_payload)).setText(string);
                        if(splited[8].equals("error") || splited[8].equals("fatal")) {
                            ((TextView)row.findViewById(R.id.log_payload)).setBackgroundResource(R.drawable.border_red);
                        }
                        if(splited[8].equals("warn")) {
                            ((TextView)row.findViewById(R.id.log_payload)).setBackgroundResource(R.drawable.border_yellow);
                        }
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

        ip = (EditText)MainActivity.this.findViewById(R.id.ip);
        ip.setText("192.168.42.210");
        SetDltServerIp(ip.getText().toString());

        checkBox = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_scroll);
        checkBox.setChecked(true);

        checkBoxConn = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_conn);
        checkBoxConn.setChecked(false);
        checkBoxConn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    SetDltServerIp(ip.getText().toString());
                    startLogs();
                }
                else {
                    StopLogs();
                }
            }
        });

        addListenerOnButton();

    }
    @Override
    public void onResume() {
        super.onResume();
        //((TextView)findViewById(R.id.hellojniMsg)).setText(stringFromJNI());
        //startLogs();

        TableLayout table = (TableLayout)MainActivity.this.findViewById(R.id.log_table);

        // Inflate your row "template" and fill out the fields.
        TableRow row = (TableRow)LayoutInflater.from(MainActivity.this).inflate(R.layout.log_row, null);
        ((TextView)row.findViewById(R.id.log_timestamp)).setText("Timestamp");
        ((TextView)row.findViewById(R.id.log_ecuid)).setText("Ecuid");
        ((TextView)row.findViewById(R.id.log_apid)).setText("Apid");
        ((TextView)row.findViewById(R.id.log_ctid)).setText("Ctid");
        ((TextView)row.findViewById(R.id.log_subtype)).setText("Type");
        ((TextView)row.findViewById(R.id.log_payload)).setText("Payload");
        table.addView(row);

        table.requestLayout();     // Not sure if this is needed.

        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause () {
        super.onPause();
        //StopLogs();

        Log.i(TAG, "onPause");
    }  

    public void addListenerOnButton() {

        Button button = (Button) findViewById(R.id.save);

        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                writeToFile();
            }

        });

    }

    public void writeToFile()
    {
        // Get the directory for the user's public pictures directory.
        final File path 
            = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS); 

        // Make sure the path directory exists.
        if(!path.exists())
        {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = "dlt-" 
                        + ip.getText().toString() 
                        + "-" 
                        + mdformat.format(calendar.getTime()) 
                        + ".log";
        final File file = new File(path, filename);

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            TableLayout table = (TableLayout)MainActivity.this.findViewById(R.id.log_table);
            for(int i = 0, j = table.getChildCount(); i < j; i++) {
                View view = table.getChildAt(i);
                if (view instanceof TableRow) {
                    // then, you can remove the the row you want...
                    // for instance...
                    TableRow row = (TableRow) view;
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_timestamp)).getText().toString() + " ");
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_ecuid)).getText().toString() + " ");
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_apid)).getText().toString() + " ");
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_ctid)).getText().toString() + " ");
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_subtype)).getText().toString() + " ");
                    myOutWriter.append(((TextView)row.findViewById(R.id.log_payload)).getText().toString()+ "\n");
                }
            }
            

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            Toast.makeText(getBaseContext(), 
                            "File saved : " + path + "/" +filename,
                                                Toast.LENGTH_LONG).show();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        } 
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
        // if (txt.toLowerCase().contains("error")) {
        //     Log.e(TAG, "Native Err: " + txt);
        // } else {
             Log.i(TAG, "Native Msg: " + txt);
        // }
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
