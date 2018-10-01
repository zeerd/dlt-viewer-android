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
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ControlActivity extends Activity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.control);

	}

    public void setSpecialLogLevel(View v) {
        EditText apid = (EditText)ControlActivity.this.findViewById(R.id.special_apid);
        EditText ctid = (EditText)ControlActivity.this.findViewById(R.id.special_ctid);
        EditText lvl = (EditText)ControlActivity.this.findViewById(R.id.special_level);

        setLevel(
            apid.getText().toString(),
            ctid.getText().toString(),
            Integer.parseInt(lvl.getText().toString())
            );
        Toast.makeText(getBaseContext(),
            "Set the level of ["
            + apid.getText().toString()
            + ":"
            + ctid.getText().toString()
            + "] to be "
            + lvl.getText().toString(),
            Toast.LENGTH_SHORT).show();
    }

    public void setDefaultLogLevel(View v) {
        EditText lvl = (EditText)ControlActivity.this.findViewById(R.id.default_level);
        String sLvl = lvl.getText().toString();
        setDefaultLevel(Integer.parseInt(sLvl));

        Toast.makeText(getBaseContext(),
            "Set the default level to be "
            + lvl.getText().toString(),
            Toast.LENGTH_SHORT).show();
    }

    public void sendInjectMessage(View v) {
        EditText apid = (EditText)ControlActivity.this.findViewById(R.id.inject_apid);
        EditText ctid = (EditText)ControlActivity.this.findViewById(R.id.inject_ctid);
        EditText sid = (EditText)ControlActivity.this.findViewById(R.id.inject_sid);
        EditText msg = (EditText)ControlActivity.this.findViewById(R.id.inject_msg);
        CheckBox hex = (CheckBox)ControlActivity.this.findViewById(R.id.checkbox_hex);

        sendInject(
            apid.getText().toString(),
            ctid.getText().toString(),
            Integer.parseInt(sid.getText().toString()),
            msg.getText().toString(),
            hex.isChecked()?1:0
            );

        Toast.makeText(getBaseContext(),
            "Send inject message to ["
            + apid.getText().toString()
            + ":"
            + ctid.getText().toString()
            + ":"
            + sid.getText().toString()
            + "] : "
            + msg.getText().toString(),
            Toast.LENGTH_SHORT).show();
    }

    public void gotoLogsLine(View v) {
        EditText line = (EditText)ControlActivity.this.findViewById(R.id.goto_line);

        MainActivity.search_index = Integer.parseInt(line.getText().toString());

        Toast.makeText(getBaseContext(),
                getResources().getString(R.string.goto_line)
                + " : " + MainActivity.search_index,
            Toast.LENGTH_SHORT).show();

        returnControl(v);
    }

    public void returnControl(View v) {
    	finish();
    }

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native void setDefaultLevel(int level);
    public native void setLevel(String apid, String ctid, int level);
    public native void sendInject(String apid, String ctid, int sid, String msg, int hex);

}
