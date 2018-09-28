/*
 * @licence app begin@
 *
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License (MPL), v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * @licence end@
 */

package com.zeerd.dltviewer;

//import android.support.annotation.Keep;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import com.zeerd.dltviewer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private int type = 2; /* 0=header, 1=payload, 2=msg */
    private String sHeader= "";
    private ScrollView scrollView;
    private CheckBox checkBox;
    private CheckBox checkBoxConn;
    private EditText ip;
    private String dltFile;
    private ListView listviewLogTable;
    private LogTableAdapter adapterLogs;
    private MyWorkerThread workerThread = null;
    private TableInitTask initTask;

    public static List<LogRow> rtLogsList;
    public static int search_index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rtLogsList = new ArrayList<LogRow>();

        workerThread = new MyWorkerThread();
        workerThread.start();

        setContentView(R.layout.activity_main);
        scrollView = (ScrollView)MainActivity.this.findViewById(R.id.log_scroll);

        ip = (EditText)MainActivity.this.findViewById(R.id.ip);
        String wifiIP = getDeviceWiFiIP();
        if(!wifiIP.equals("0.0.0.1")) {
            ip.setText(wifiIP);
        }
        else {
            ip.setText("192.168.42.210");
        }
        setDltServerIp(ip.getText().toString());

        checkBox = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_scroll);
        checkBox.setChecked(false);

        checkBoxConn = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_conn);
        checkBoxConn.setChecked(false);
        checkBoxConn.setOnCheckedChangeListener(connListener);

        CheckBox checkBoxFile = (CheckBox)MainActivity.this.findViewById(R.id.checkbox_file);
        checkBoxFile.setChecked(false);
        checkBoxFile.setOnCheckedChangeListener(fileListener);

        addListenerOnButton();

        String filter = getExternalCacheDir() + "/my.filter";
        setDltServerFilter(filter);

        listviewLogTable = (ListView)findViewById(R.id.log_table);
        adapterLogs = new LogTableAdapter(this);
        listviewLogTable.setAdapter(adapterLogs);

        listviewLogTable.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_MOVE) {
                    checkBox.setChecked(false);
                }
                return false;
            }
        });

        search_index = -1;

        Timer timer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                initTask = new TableInitTask(adapterLogs);
                initTask.execute();
            }
        };
        timer.scheduleAtFixedRate(t,500,500);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(search_index > 0 && search_index < adapterLogs.getCount()) {
            listviewLogTable.setSelection(search_index - 1);
            checkBox.setChecked(false);
        }
    }

    private CompoundButton.OnCheckedChangeListener connListener =
        new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    setDltServerIp(ip.getText().toString());
                    startLogs();
                }
                else {
                    stopLogs();
                }
            }
        };

    private CompoundButton.OnCheckedChangeListener fileListener =
        new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat mdformat = new SimpleDateFormat("yyyyMMddHHmmss");
                    File path
                        = new File(Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOWNLOADS)
                        , "/dlt");
                    if(!path.exists())
                    {
                        // Make it, if it doesn't exit
                        path.mkdirs();
                    }
                    dltFile = path + "/dlt-"
                                   + ip.getText().toString()
                                   + "-"
                                   + mdformat.format(calendar.getTime())
                                   + ".dlt";
                    startRecordLogs(dltFile);
                    Toast.makeText(getBaseContext(),
                            "Start to save logs : " + dltFile,
                                                Toast.LENGTH_LONG).show();
                }
                else {
                    stopRecordLogs();
                    Toast.makeText(getBaseContext(),
                            "File saved : " + dltFile,
                                                Toast.LENGTH_LONG).show();
                }
            }
        };

    public void clearLogTable(View v) {
        rtLogsList.clear();
        adapterLogs.notifyDataSetChanged();
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v, Gravity.NO_GRAVITY);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.setting, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.filter: {
                        Intent intent = new Intent(getBaseContext(), SettingActivity.class);
                        startActivity(intent);
                    }
                    return true;
                    case R.id.control: {
                        Intent intent = new Intent(getBaseContext(), ControlActivity.class);
                        startActivity(intent);
                    }
                    return true;
                    case R.id.search: {
                        Intent intent = new Intent(getBaseContext(), SearchActivity.class);
                        startActivity(intent);
                    }
                    return true;
                    case R.id.help: {
                        Intent intent = new Intent(getBaseContext(), HelpActivity.class);
                        startActivity(intent);
                    }
                    return true;
                    default:
                    return false;
                }
            }
        });
        popup.show();
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
            = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS) , "/dlt");

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

            for (int i = 0; i < rtLogsList.size(); i++) {
                LogRow row = rtLogsList.get(i);
                myOutWriter.append(row.getColumn(LogRow.ROW_TIMESTAMP) + " ");
                myOutWriter.append(row.getColumn(LogRow.ROW_ECUID) + " ");
                myOutWriter.append(row.getColumn(LogRow.ROW_APID) + " ");
                myOutWriter.append(row.getColumn(LogRow.ROW_CTID) + " ");
                myOutWriter.append(row.getColumn(LogRow.ROW_SUBTYPE) + " ");
                myOutWriter.append(row.getColumn(LogRow.ROW_PAYLOAD) + "\n");
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

    public String getDeviceWiFiIP()
    {
        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        String[] ips = Formatter.formatIpAddress(ip).split("\\.");
        return ips [0] + "." + ips [1] + "." + ips [2] + ".1";
    }

    // This thread use to update the listview's adapter.
    // We could not update it directly, the burst data will crash the main thread.
    // This thread will be pulled up each 500ms by timer.
    private class TableInitTask extends AsyncTask<Void,Integer, List<LogRow> > {

        private static final String TAG = "DLT-Viewer";

        private LogTableAdapter mTableAdapter;

        public TableInitTask(LogTableAdapter tableAdapter) {
            super();
            mTableAdapter = tableAdapter;
        }

        @Override
        protected List<LogRow> doInBackground(Void... params) {
            // Log.i(TAG, "TableInitTask::doInBackground()");

            List<LogRow> data;
            if(rtLogsList == null) {
                data = new ArrayList<LogRow>();
            }
            else {
                data = new ArrayList<>(rtLogsList);
            }

            return data;
        }

        @Override
        protected void onPostExecute(List<LogRow> result) {

            // Log.i(TAG, "TableInitTask::onPostExecute()");

            mTableAdapter.setData(result);
            mTableAdapter.notifyDataSetChanged();
            if(checkBox.isChecked()) {
                listviewLogTable.post(new Runnable() {
                    @Override
                    public void run() {
                        listviewLogTable.setSelection(adapterLogs.getCount() - 1);
                    }
                });
            }
        }
    }

    public void parseMessages(String string) {
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

                if(!splited[7].equals("control")) {

                    rtLogsList.add(new LogRow(
                                        ""+(rtLogsList.size()+1),
                                        splited[2],
                                        splited[4],
                                        splited[5],
                                        splited[6],
                                        splited[8],
                                        string));

                }
            }
            type = 2;
        }
    }

    // This child thread class has it's own Looper and Handler object.
    // We use it to receive the logs from jni to reduce the workload of main thread.
    private class MyWorkerThread extends Thread{
        // This is worker thread handler.
        public Handler workerThreadHandler;

        @Override
        public void run() {
            // Prepare child thread Lopper object.
            Looper.prepare();

            // Create child thread Handler.
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    String string = bundle.getString("msg");

                    parseMessages(string);

                }
            };
            staticHandler = handler;

            // Loop the child thread message queue.
            Looper.loop();

        }
    }

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native void startLogs();
    public native void stopLogs();
    public native void setDltServerIp(String ip);
    public native void setDltServerFilter(String file);
    public native void startRecordLogs(String file);
    public native void stopRecordLogs();

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
        //     Log.i(TAG, "Native Msg: " + txt);
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
