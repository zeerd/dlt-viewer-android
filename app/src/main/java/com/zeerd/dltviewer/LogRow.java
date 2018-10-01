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

public class LogRow {

    static final int ROW_INDEX = 0;
    static final int ROW_TIMESTAMP = 1;
    static final int ROW_ECUID = 2;
    static final int ROW_APID = 3;
    static final int ROW_CTID = 4;
    static final int ROW_SUBTYPE = 5;
    static final int ROW_PAYLOAD = 6;
    static final int ROW_COUNT = 7;

    private String[] column = new String[ROW_COUNT];

    LogRow(
            String index,
            String timestamp,
            String ecuid,
            String apid,
            String ctid,
            String subtype,
            String payload) {

        super();
        this.column[ROW_INDEX] = index;
        this.column[ROW_TIMESTAMP] = timestamp;
        this.column[ROW_ECUID] = ecuid;
        this.column[ROW_APID] = apid;
        this.column[ROW_CTID] = ctid;
        this.column[ROW_SUBTYPE] = subtype;
        this.column[ROW_PAYLOAD] = payload;
    }

    String getColumn(int index) {
        return column[index];
    }
    public void setColumn(int index, String str) {
        this.column[index] = str;
    }

    int getIndex() {
        return Integer.parseInt(this.column[ROW_INDEX]);
    }
}
