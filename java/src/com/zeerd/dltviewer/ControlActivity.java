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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.zeerd.dltviewer.R;

public class ControlActivity extends Activity {

    private static final String TAG = "DLT-Viewer";
    private static final String [] langurage ={"off","fatal","error","warn","info","debug","Verbose"};

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
            Toast.LENGTH_LONG).show();
    }

    public void setDefaultLogLevel(View v) {
        EditText lvl = (EditText)ControlActivity.this.findViewById(R.id.default_level);
        String sLvl = lvl.getText().toString();
        setDefaultLevel(Integer.parseInt(sLvl));

        Toast.makeText(getBaseContext(),
            "Set the default level to be "
            + lvl.getText().toString(),
            Toast.LENGTH_LONG).show();
    }

    public void returnControl(View v) {
    	finish();
    }

    static {
        System.loadLibrary("dlt-jnicallback");
    }
    public native void setDefaultLevel(int level);
    public native void setAllLevel(int level);
    public native void setLevel(String apid, String ctid, int level);

}