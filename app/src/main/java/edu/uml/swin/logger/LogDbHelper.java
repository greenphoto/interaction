package edu.uml.swin.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import edu.uml.swin.logger.LogContract.LogEntry;

/**
 * Created by admin on 1/31/15.
 */
public class LogDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "interactionLog.db";

    private static final String TEXT_TYPES = " TEXT";
    private static final String INTEGER_TYPES = " INTEGER";
    private static final String COMMA_SEP = " , ";
    private static final String SQL_CREATE_EVENT_TABLE = "CREATE TABLE " + LogEntry.EVENT_TABLE_NAME
            + " (" + LogEntry._ID + " INTEGER PRIMARY KEY, "
            + LogEntry.COLUMN_NAME_EVENT_TYPE + TEXT_TYPES+ COMMA_SEP
            + LogEntry.COLUMN_NAME_EVENT_SOURCE + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_PKG_NAME + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_EVENT_TIME + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_SYSTEM_TIME + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_EVENT_TEXT + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_WINDOW_ID + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_INDEX_ZERO + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_INDEX_ONE + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_INDEX_TWO + INTEGER_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_SOURCE_CLASS + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_VIEW_RESOURCE_ID + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_BOUNDS_IN_PARENT + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_BOUNDS_IN_SCREEN + TEXT_TYPES + COMMA_SEP
            + LogEntry.COLUMN_NAME_WINDOW_INFO + TEXT_TYPES + " )";

    private static final String SQL_DELETE_EVENT_TABLE = "DROP TABLE IF EXISTS " + LogEntry.EVENT_TABLE_NAME;

    public LogDbHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EVENT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
