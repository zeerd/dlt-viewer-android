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

import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LogTableAdapter extends BaseAdapter {
    private Activity activity;
    private List<LogRow> rows;
    private static final String TAG = "DLT-Viewer";

    public LogTableAdapter(Activity activity, List<LogRow> rows) {
        super();
        this.activity = activity;
        this.rows = rows;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.log_row, null);
        }
        TextView col1 = (TextView) convertView.findViewById(R.id.log_timestamp);
        TextView col2 = (TextView) convertView.findViewById(R.id.log_ecuid);
        TextView col3 = (TextView) convertView.findViewById(R.id.log_apid);
        TextView col4 = (TextView) convertView.findViewById(R.id.log_ctid);
        TextView col5 = (TextView) convertView.findViewById(R.id.log_subtype);
        TextView col6 = (TextView) convertView.findViewById(R.id.log_payload);

        String timestamp = rows.get(position).getColumn(0);
        String subtype = rows.get(position).getColumn(4);

        int len = timestamp.length();
        String new_timestamp = timestamp.substring(0, len-4) + "." + timestamp.substring(len-4, len);
        Log.i(TAG, timestamp+" vs "+new_timestamp);
        col1.setText(new_timestamp);
        col2.setText(rows.get(position).getColumn(1));
        col3.setText(rows.get(position).getColumn(2));
        col4.setText(rows.get(position).getColumn(3));
        col5.setText(subtype);
        col6.setText(rows.get(position).getColumn(5));

        if(subtype.equals("error") || subtype.equals("fatal")) {
            col1.setBackgroundResource(R.drawable.border_red);
            col2.setBackgroundResource(R.drawable.border_red);
            col3.setBackgroundResource(R.drawable.border_red);
            col4.setBackgroundResource(R.drawable.border_red);
            col5.setBackgroundResource(R.drawable.border_red);
            col6.setBackgroundResource(R.drawable.border_red);
        }
        else if(subtype.equals("warn")) {
            col1.setBackgroundResource(R.drawable.border_yellow);
            col2.setBackgroundResource(R.drawable.border_yellow);
            col3.setBackgroundResource(R.drawable.border_yellow);
            col4.setBackgroundResource(R.drawable.border_yellow);
            col5.setBackgroundResource(R.drawable.border_yellow);
            col6.setBackgroundResource(R.drawable.border_yellow);
        }
        else {
            col1.setBackgroundResource(R.drawable.border);
            col2.setBackgroundResource(R.drawable.border);
            col3.setBackgroundResource(R.drawable.border);
            col4.setBackgroundResource(R.drawable.border);
            col5.setBackgroundResource(R.drawable.border);
            col6.setBackgroundResource(R.drawable.border);
        }

        return convertView;
    }
}