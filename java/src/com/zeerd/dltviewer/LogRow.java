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

import java.util.ArrayList;
import java.util.List;

public class LogRow {

    private String[] column = new String[7];

    public static final int ROW_INDEX = 0;
    public static final int ROW_TIMESTAMP = 1;
    public static final int ROW_ECUID = 2;
    public static final int ROW_APID = 3;
    public static final int ROW_CTID = 4;
    public static final int ROW_SUBTYPE = 5;
    public static final int ROW_PAYLOAD = 6;

    public LogRow(
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

    public String getColumn(int index) {
        return column[index];
    }
    public void setColumn(int index, String str) {
        this.column[index] = str;
    }

    public int getIndex() {
        return Integer.parseInt(this.column[ROW_INDEX]);
    }
}