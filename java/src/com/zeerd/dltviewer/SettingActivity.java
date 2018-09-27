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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import com.zeerd.dltviewer.R;

public class SettingActivity extends Activity {

    private TableLayout table;
    private String filterPath;
    private static final String TAG = "DLT-Viewer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        table = (TableLayout)SettingActivity.this.findViewById(R.id.filter_table);

        filterPath = getExternalCacheDir() + "";

        loadFilter(null);
    }

    public void clearFilter(View v) {
        for(int i = 1, j = table.getChildCount(); i < j; i++) {
            View view = table.getChildAt(i);
            if (view instanceof TableRow) {
                // then, you can remove the the row you want...
                // for instance...
                TableRow row = (TableRow) view;
                table.removeView(row);
            }
        }
    }

    public void saveFilter(View v) {
        Log.i(TAG, "save filter into " + filterPath + "/my.filter");

        final File file = new File(filterPath, "my.filter");

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            for(int i = 1, j = table.getChildCount(); i < j; i++) {
                View view = table.getChildAt(i);
                if (view instanceof TableRow) {
                    // then, you can remove the the row you want...
                    // for instance...
                    TableRow row = (TableRow) view;
                    String r = ((EditText)row.findViewById(R.id.filter_apid)).getText().toString();
                    if(r.equals("")) {
                        myOutWriter.append("----\n");
                    }
                    else {
                        myOutWriter.append(r + "\n");
                    }
                    r = ((EditText)row.findViewById(R.id.filter_ctid)).getText().toString();
                    if(r.equals("")) {
                        myOutWriter.append("----\n");
                    }
                    else {
                        myOutWriter.append(r + "\n");
                    }
                }
            }


            myOutWriter.close();

            fOut.flush();
            fOut.close();

            Toast.makeText(getBaseContext(),
                            "Filter saved : " + filterPath + "/my.filter",
                                                Toast.LENGTH_LONG).show();

            setDltServerFilter(filterPath + "/my.filter");
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    public void addFilter(View v) {

        // Inflate your row "template" and fill out the fields.
        TableRow row = (TableRow)LayoutInflater.from(SettingActivity.this).inflate(R.layout.filter_row, null);
        ((EditText)row.findViewById(R.id.filter_apid)).setText("----");
        ((EditText)row.findViewById(R.id.filter_ctid)).setText("----");

        table.addView(row);

        table.requestLayout();     // Not sure if this is needed.
    }

    public void removeFilter(View v) {
        TableRow tr = (TableRow) v.getParent();
        table.removeView(tr);
    }

    public void loadFilter(View v) {
        Log.i(TAG, "load filter from " + filterPath);

        clearFilter(v);
        //Get the text file
        File file = new File(filterPath, "my.filter");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String apid, ctid;

            while ((apid = br.readLine()) != null) {
                ctid = br.readLine();
                if(ctid != null) {
                    TableRow row = (TableRow)LayoutInflater.from(SettingActivity.this).inflate(R.layout.filter_row, null);
                    ((EditText)row.findViewById(R.id.filter_apid)).setText(apid);
                    ((EditText)row.findViewById(R.id.filter_ctid)).setText(ctid);
                    table.addView(row);
                    table.requestLayout();     // Not sure if this is needed.
                }
            }
            br.close();

            Toast.makeText(getBaseContext(),
                            "Filter loaded : " + filterPath + "/" + "my.filter",
                                                Toast.LENGTH_LONG).show();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    public void returnSetting(View v) {
        finish();
    }

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native void setDltServerFilter(String file);

}