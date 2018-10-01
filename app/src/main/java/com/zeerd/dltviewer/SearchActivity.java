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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity {

    private LogTableAdapter adapterLogs;
    private SharedPreferences prefs;
    ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.search);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        ListView listviewResultTable = (ListView) findViewById(R.id.result_table);

        adapterLogs = new LogTableAdapter(this);
        listviewResultTable.setAdapter(adapterLogs);

        prefs = this.getSharedPreferences(
                                "com.zeerd.dltviewer", Context.MODE_PRIVATE);
        String kw = prefs.getString("com.zeerd.dltviewer.keyword", "");
        ((EditText)findViewById(R.id.keyword)).setText(kw);

        registerForContextMenu(listviewResultTable);

	}

    public void searchFromLogs(View v) {

        prefs.edit().putString(
            "com.zeerd.dltviewer.keyword",
            ((EditText)findViewById(R.id.keyword)).getText().toString()).apply();
        TableInitTask initTask = new TableInitTask(
                adapterLogs, MainActivity.rtLogsList.size(), progressBar);
        initTask.execute();
    }

    public void returnSearch(View v) {
    	finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.result_table) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            LogRow row = adapterLogs.getData().get(info.position);

            MainActivity.search_index = row.getIndex();

            returnSearch(v);
        }
    }

    private class TableInitTask extends AsyncTask<Void,Integer, List<LogRow> > {

        private static final String TAG = "DLT-Viewer";

        private LogTableAdapter mTableAdapter;
        private ProgressBar mProgressBar;
        private int mLineCount;

        TableInitTask(LogTableAdapter tableAdapter, int lineCount, ProgressBar progressBar) {
            super();
            mTableAdapter = tableAdapter;
            mProgressBar = progressBar;
            mLineCount = lineCount;
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "TableInitTask::onPreExecute()");

            mProgressBar.setMax(mLineCount);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<LogRow> doInBackground(Void... params) {
            Log.i(TAG, "TableInitTask::doInBackground()");

            EditText kw = (EditText)findViewById(R.id.keyword);
            List<LogRow> data = new ArrayList<>();

            String keyword = kw.getText().toString().toLowerCase();
            for (int i = 0; i < MainActivity.rtLogsList.size(); i++) {
                LogRow row = MainActivity.rtLogsList.get(i);

                String payload = row.getColumn(LogRow.ROW_PAYLOAD).toLowerCase();
                if(payload.indexOf(keyword, 0) != -1) {
                    data.add(new LogRow(
                                    row.getColumn(LogRow.ROW_INDEX),
                                    row.getColumn(LogRow.ROW_TIMESTAMP),
                                    row.getColumn(LogRow.ROW_ECUID),
                                    row.getColumn(LogRow.ROW_APID),
                                    row.getColumn(LogRow.ROW_CTID),
                                    row.getColumn(LogRow.ROW_SUBTYPE),
                                    row.getColumn(LogRow.ROW_PAYLOAD)));
                }
            }

            return data;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<LogRow> result) {

            Log.i(TAG, "TableInitTask::onPostExecute()");

            mProgressBar.setProgress(mLineCount);
            mProgressBar.setVisibility(View.GONE);

            mTableAdapter.setData(result);
            mTableAdapter.notifyDataSetChanged();

            Toast.makeText(getBaseContext(),
                getResources().getString(R.string.search_finish),
                Toast.LENGTH_SHORT).show();

        }
    }
}