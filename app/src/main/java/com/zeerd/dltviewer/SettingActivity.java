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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class SettingActivity extends Activity {

    private static final String TAG = "DLT-Viewer";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        prefs = this.getSharedPreferences(
                "com.zeerd.dltviewer", Context.MODE_PRIVATE);
        String target = prefs.getString("com.zeerd.dltviewer.target", "192.168.42.210");
        ((EditText)findViewById(R.id.default_target)).setText(target);
        String ecu = prefs.getString("com.zeerd.dltviewer.ecu", "RECV");
        ((EditText)findViewById(R.id.default_ecu)).setText(ecu);

        Switch onOffSwitch = (Switch)  findViewById(R.id.debug_switch);
        boolean dbg = prefs.getBoolean("com.zeerd.dltviewer.debug", false);
        onOffSwitch.setChecked(dbg);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(
                        "com.zeerd.dltviewer.debug",
                        isChecked).apply();
                Log.i(TAG, "set debug report to " + isChecked);
            }
        });
    }

    public void setDefaultTarget(View v) {
        String target = ((EditText)findViewById(R.id.default_target)).getText().toString();
        prefs.edit().putString(
                "com.zeerd.dltviewer.target",
                target).apply();
        Log.i(TAG, "set default target to " + target);
    }

    public void setDefaultEcu(View v) {
        String ecu = ((EditText)findViewById(R.id.default_ecu)).getText().toString();
        prefs.edit().putString(
                "com.zeerd.dltviewer.ecu",
                ecu).apply();
        setEcuID(ecu);
        Log.i(TAG, "set ecu to " + ecu);
    }

    public void returnSetting(View v) {
        finish();
    }

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native void setEcuID(String ecuid);
}
