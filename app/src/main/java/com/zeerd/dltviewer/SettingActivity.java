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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SettingActivity extends Activity {

    private TableLayout table;
    private String filterPath;
    private String filterName = "my";

    private static final String TAG = "DLT-Viewer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        table = (TableLayout)SettingActivity.this.findViewById(R.id.filter_table);

        filterPath = getExternalCacheDir() + "";

        loadFilterFile(null);
    }

    public void clearFilter(View v) {
        // for(int i = 0, j = table.getChildCount(); i < j; i++) {
        //     View view = table.getChildAt(i);
        //     if (view instanceof TableRow) {
        //         // then, you can remove the the row you want...
        //         // for instance...
        //         TableRow row = (TableRow) view;
        //         table.removeView(row);
        //     }
        // }
        table.removeAllViews();
    }

    public void deleteFilter(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(
            getResources().getString(R.string.del) + " " + filterName + " ?");

        // Set up the buttons
        builder.setPositiveButton(
                    getResources().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final File file = new File(filterPath, filterName + ".filter");
                file.delete();
            }
        });
        builder.setNegativeButton(
                    getResources().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void saveFilterFile() {

        Log.i(TAG,
            "save filter into " + filterPath + "/" + filterName + ".filter");

        final File file = new File(filterPath, filterName + ".filter");

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
                    EditText apid = (EditText)row.findViewById(R.id.filter_apid);
                    String r = apid.getText().toString();
                    if(r.equals("")) {
                        myOutWriter.append("----\n");
                    }
                    else {
                        myOutWriter.append(r).append("\n");
                    }
                    EditText ctid = (EditText)row.findViewById(R.id.filter_ctid);
                    r = ctid.getText().toString();
                    if(r.equals("")) {
                        myOutWriter.append("----\n");
                    }
                    else {
                        myOutWriter.append(r).append("\n");
                    }
                }
            }


            myOutWriter.close();

            fOut.flush();
            fOut.close();

            Toast.makeText(getBaseContext(),
                            getResources().getString(R.string.filter_save)
                            + " : " + filterPath + "/" + filterName + ".filter",
                            Toast.LENGTH_SHORT).show();

            setDltServerFilter(filterPath + "/" + filterName + ".filter");
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    public void saveFilter(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.input_filename));

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(filterName);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(
                    getResources().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filterName = input.getText().toString();
                saveFilterFile();
            }
        });
        builder.setNegativeButton(
                    getResources().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    public void addFilter(View v) {

        // Inflate your row "template" and fill out the fields.
        TableRow row = (TableRow)LayoutInflater
                            .from(SettingActivity.this)
                            .inflate(R.layout.filter_row, null);
        ((EditText)row.findViewById(R.id.filter_apid)).setText("----");
        ((EditText)row.findViewById(R.id.filter_ctid)).setText("----");

        table.addView(row);

        table.requestLayout();     // Not sure if this is needed.
    }

    public void removeFilter(View v) {
        TableRow tr = (TableRow) v.getParent();
        table.removeView(tr);
    }

    public void loadFilterFile(View v) {
        Log.i(TAG, "load filter from " + filterPath);

        clearFilter(v);
        //Get the text file
        File file = new File(filterPath, filterName + ".filter");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String apid, ctid;

            while ((apid = br.readLine()) != null) {
                ctid = br.readLine();
                if(ctid != null) {
                    TableRow row = (TableRow)LayoutInflater
                                        .from(SettingActivity.this)
                                        .inflate(R.layout.filter_row, null);
                    ((EditText)row.findViewById(R.id.filter_apid)).setText(apid);
                    ((EditText)row.findViewById(R.id.filter_ctid)).setText(ctid);
                    table.addView(row);
                    table.requestLayout();     // Not sure if this is needed.
                }
            }
            br.close();

            Toast.makeText(getBaseContext(),
                            getResources().getString(R.string.filter_load)
                            + " : " + filterPath + "/" + filterName + ".filter",
                            Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    public void loadFilter(View v) {

        File directory = new File(filterPath);
        File[] files = directory.listFiles();
        if(files.length > 0) {
            PopupMenu popup = new PopupMenu(this, v);
            for (File file : files) {
                String name = file.getName();
                if (name.substring(name.lastIndexOf('.')).equals(".filter")) {
                    popup.getMenu().add(name.substring(0, name.lastIndexOf('.')));
                }
            }
            popup.setOnMenuItemClickListener(
                            new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    filterName = item.getTitle().toString();
                    loadFilterFile(null);
                    return true;
                }
            });
            popup.show();
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