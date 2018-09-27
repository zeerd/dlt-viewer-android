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

    private String[] column = new String[6];

    public LogRow(
                String timestamp,
                String ecuid,
                String apid,
                String ctid,
                String subtype,
                String payload) {

        super();
        this.column[0] = timestamp;
        this.column[1] = ecuid;
        this.column[2] = apid;
        this.column[3] = ctid;
        this.column[4] = subtype;
        this.column[5] = payload;
    }

    public String getColumn(int index) {
        return column[index];
    }
    public void setColumn(int index, String str) {
        this.column[index] = str;
    }
}