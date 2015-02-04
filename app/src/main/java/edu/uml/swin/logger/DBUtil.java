package edu.uml.swin.logger;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by admin on 2/4/15.
 */
public class DBUtil {

    private LogDbHelper dbHelper;
    private SQLiteDatabase db;

    public DBUtil(LogDbHelper dbHelper){
        this.dbHelper = dbHelper;
        db = dbHelper.getWritableDatabase();
    }

    public void writeToDebugDB(long sysTime, String entry){
        String calendarTime = Utils.getReadableTime(sysTime);
        ContentValues values = new ContentValues();
        values.put(LogContract.LogEntry.COLUMN_NAME_SYSTEM_TIME, sysTime);
        values.put(LogContract.LogEntry.COLUMN_NAME_DEBUG_INFO, entry);
        values.put(LogContract.LogEntry.COLUMN_NAME_CALENDAR_TIME, calendarTime);
        db.insert(LogContract.LogEntry.DEBUG_INFO_TABLE_NAME, null, values);
    }


}
