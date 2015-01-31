package edu.uml.swin.logger;

import android.provider.BaseColumns;

/**
 * Created by admin on 1/31/15.
 */
public final class LogContract {

    public LogContract(){}

    public static abstract class LogEntry implements BaseColumns{
        public static final String EVENT_TABLE_NAME = "events";
//        public static final String WINDOW_TABLE_NAME = "windows";

        public static final String COLUMN_NAME_EVENT_TYPE = "event_type";
        public static final String COLUMN_NAME_EVENT_SOURCE = "event_source";
        public static final String COLUMN_NAME_PKG_NAME = "pkg_name";
        public static final String COLUMN_NAME_EVENT_TIME = "event_time";
        public static final String COLUMN_NAME_SYSTEM_TIME = "sys_time";
        public static final String COLUMN_NAME_EVENT_TEXT = "event_text";
        public static final String COLUMN_NAME_WINDOW_ID = "window_id";
        public static final String COLUMN_NAME_INDEX_ZERO = "index_0";
        public static final String COLUMN_NAME_INDEX_ONE = "index_1";
        public static final String COLUMN_NAME_INDEX_TWO = "index_2";
        public static final String COLUMN_NAME_SOURCE_CLASS = "source_class";
        public static final String COLUMN_NAME_VIEW_RESOURCE_ID = "view_resource_id";
        public static final String COLUMN_NAME_BOUNDS_IN_PARENT = "bounds_in_parent";
        public static final String COLUMN_NAME_BOUNDS_IN_SCREEN = "bounds_in_screen";
        public static final String _ID = "id";
        public static final String COLUMN_NAME_WINDOW_INFO = "window_info";

    }



}
