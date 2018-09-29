/*
 * @licence app begin@
 *
 * Copyright (C) 2018, Charles Chan <emneg@zeerd.com>
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

    private static class ViewHolder {
        private TextView[] column = new TextView[LogRow.ROW_COUNT];
    }

    public LogTableAdapter(Activity activity, List<LogRow> rows) {
        super();
        this.activity = activity;
        this.rows = rows;
    }

    public LogTableAdapter(Activity activity) {
        super();
        this.activity = activity;
    }

    public void setData(List<LogRow> rows) {
        this.rows = rows;
    }

    public List<LogRow> getData() {
        return this.rows;
    }

    @Override
    public int getCount() {
        if(rows == null) {
            return 0;
        }
        else {
            return rows.size();
        }
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

        int i;
        LogRow logRow = (LogRow)getItem(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.log_row, null);

            viewHolder.column[LogRow.ROW_INDEX] = (TextView) convertView.findViewById(R.id.log_index);
            viewHolder.column[LogRow.ROW_TIMESTAMP] = (TextView) convertView.findViewById(R.id.log_timestamp);
            viewHolder.column[LogRow.ROW_ECUID] = (TextView) convertView.findViewById(R.id.log_ecuid);
            viewHolder.column[LogRow.ROW_APID] = (TextView) convertView.findViewById(R.id.log_apid);
            viewHolder.column[LogRow.ROW_CTID] = (TextView) convertView.findViewById(R.id.log_ctid);
            viewHolder.column[LogRow.ROW_SUBTYPE] = (TextView) convertView.findViewById(R.id.log_subtype);
            viewHolder.column[LogRow.ROW_PAYLOAD] = (TextView) convertView.findViewById(R.id.log_payload);

            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String timestamp = rows.get(position).getColumn(LogRow.ROW_TIMESTAMP);
        String subtype = rows.get(position).getColumn(LogRow.ROW_SUBTYPE);

        int len = timestamp.length();
        String new_timestamp = timestamp.substring(0, len-4) + "." + timestamp.substring(len-4, len);
        // Log.i(TAG, timestamp+" vs "+new_timestamp);

        viewHolder.column[LogRow.ROW_INDEX].setText(rows.get(position).getColumn(LogRow.ROW_INDEX));
        viewHolder.column[LogRow.ROW_TIMESTAMP].setText(new_timestamp);
        viewHolder.column[LogRow.ROW_ECUID].setText(rows.get(position).getColumn(LogRow.ROW_ECUID));
        viewHolder.column[LogRow.ROW_APID].setText(rows.get(position).getColumn(LogRow.ROW_APID));
        viewHolder.column[LogRow.ROW_CTID].setText(rows.get(position).getColumn(LogRow.ROW_CTID));
        viewHolder.column[LogRow.ROW_SUBTYPE].setText(subtype);
        viewHolder.column[LogRow.ROW_PAYLOAD].setText(rows.get(position).getColumn(LogRow.ROW_PAYLOAD));

        if(subtype.equals("error") || subtype.equals("fatal")) {
            for(i=0;i<LogRow.ROW_COUNT;i++) {
                viewHolder.column[i].setBackgroundResource(R.drawable.border_red);
            }
        }
        else if(subtype.equals("warn")) {
            for(i=0;i<LogRow.ROW_COUNT;i++) {
                viewHolder.column[i].setBackgroundResource(R.drawable.border_yellow);
            }
        }
        else {
            for(i=0;i<LogRow.ROW_COUNT;i++) {
                viewHolder.column[i].setBackgroundResource(R.drawable.border);
            }
        }

        return convertView;
    }
}