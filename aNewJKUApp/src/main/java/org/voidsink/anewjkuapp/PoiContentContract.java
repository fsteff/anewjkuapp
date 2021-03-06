/*
 *       ____.____  __.____ ___     _____
 *      |    |    |/ _|    |   \   /  _  \ ______ ______
 *      |    |      < |    |   /  /  /_\  \\____ \\____ \
 *  /\__|    |    |  \|    |  /  /    |    \  |_> >  |_> >
 *  \________|____|__ \______/   \____|__  /   __/|   __/
 *                   \/                  \/|__|   |__|
 *
 *  Copyright (c) 2014-2019 Paul "Marunjar" Pretsch
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.voidsink.anewjkuapp;

import android.net.Uri;

import org.voidsink.anewjkuapp.calendar.CalendarContractWrapper;

public class PoiContentContract {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.poi";

    public static final String CONTENT_TYPE_DIR = "vnd.android.cursor.dir";
    public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item";

    public static final Uri CONTENT_URI = Uri.parse(String.format("content://%1$s",
            AUTHORITY));

    public static String getFTS() {
        return "fts4";
    }

    public static class Poi {
        public static final String PATH = "poi";
        public static final String MIMETYPE = "vnd.anewjkuapp.poi";

        public static final Uri CONTENT_URI = PoiContentContract.CONTENT_URI
                .buildUpon().appendPath(PATH).build();

        // DB Table consts
        public static final String TABLE_NAME = "poi";
        public static final String COL_ROWID = "rowid";
        public static final String COL_LAT = "latitude";
        public static final String COL_LON = "longtitude";
        public static final String COL_NAME = "name";
        public static final String COL_DESCR = "description";
        public static final String COL_ADR_STREET = "adr_street";
        public static final String COL_ADR_CITY = "adr_city";
        public static final String COL_ADR_STATE = "adr_state";
        public static final String COL_ADR_COUNTRY = "adr_country";
        public static final String COL_ADR_POSTAL_CODE = "adr_postal_code";
        public static final String COL_IS_DEFAULT = "from_user";

        public static class DB {
            public static final String[] PROJECTION = new String[]{
                    Poi.COL_ROWID,
                    Poi.COL_NAME,
                    Poi.COL_LON,
                    Poi.COL_LAT,
                    Poi.COL_DESCR,
                    Poi.COL_IS_DEFAULT};

            public static final int COL_ID = 0;
            public static final int COL_NAME = 1;
            public static final int COL_LON = 2;
            public static final int COL_LAT = 3;
            public static final int COL_DESCR = 4;
            public static final int COL_IS_DEFAULT = 5;

        }
    }

    public static Uri asEventSyncAdapter(Uri uri, String account,
                                         String accountType) {
        return uri
                .buildUpon()
                .appendQueryParameter(
                        CalendarContractWrapper.CALLER_IS_SYNCADAPTER(), "true")
                .appendQueryParameter(
                        CalendarContractWrapper.Events.ACCOUNT_NAME(), account)
                .appendQueryParameter(
                        CalendarContractWrapper.Events.ACCOUNT_TYPE(),
                        accountType).build();
    }

}
