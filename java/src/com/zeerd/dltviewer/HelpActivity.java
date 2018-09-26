package com.zeerd.dltviewer;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.zeerd.dltviewer.R;

public class HelpActivity extends Activity {

    private static final String TAG = "DLT-Viewer";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.help);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView)HelpActivity.this.findViewById(R.id.name)).setText("DLT-Viewer Android " + packageInfo.versionName);
        } catch (NameNotFoundException e) {
            ((TextView)HelpActivity.this.findViewById(R.id.name)).setText("DLT-Viewer Android");
        }
	}

    public void returnHelp(View v) {
    	finish();
    }
}