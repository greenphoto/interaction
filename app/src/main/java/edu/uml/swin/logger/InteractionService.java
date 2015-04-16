package edu.uml.swin.logger;


import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import edu.uml.swin.logger.LogContract.LogEntry;


public class InteractionService extends AccessibilityService {

    public static final String TAG = "InteractionService";
    public static final String ACTION_ENABLE = "uirecorder_enable";
    public static final String ACTION_DISABLE = "uirecorder_disable";
    private SensorLogger sensorLogger;
    private SQLiteDatabase db;
    private String currentLogEntry = "";
    private String[] pkgNames = {"org.tasks", "com.dominospizza","com.expedia.bookings","com.pinterest", "com.northpark.drinkwater","com.dancingdroid.dailysuccess"};
    private boolean sensorStopped = true;
    private static boolean loggingEnabled = false;
    private IntentFilter mMessageFilter;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long sysTime = System.currentTimeMillis();
            if (intent.getAction().equals("start_task_msg")) {
                Log.d(TAG, "=============    Clicked start button");
                writeStartLogToDB(sysTime);

            } else if (intent.getAction().equals("finish_task_msg")) {
                Log.d(TAG, "#############    Clicked finish button");
                writeFinishLogToDB(sysTime);
            }
        }
    };

    public static boolean isLoggingEnabled(){
        return loggingEnabled;
    }

    public static void enableLogging(boolean setting){
        loggingEnabled = setting;
    }

    public void onCreate() {
        super.onCreate();

        mMessageFilter = new IntentFilter();
        mMessageFilter.addAction("start_task_msg");
        mMessageFilter.addAction("finish_task_msg");
        registerReceiver(mMessageReceiver, mMessageFilter);
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(!loggingEnabled)
            return;

        long sTimeStamp = System.currentTimeMillis();

        ArrayList<String> debugInfo = new ArrayList<String>();

        if(getEventType(event).equalsIgnoreCase("Default")) return;

        AccessibilityNodeInfo source = event.getSource();
        if(source == null){
            currentLogEntry = "Failed to get source.";
            saveLogEntry(debugInfo, currentLogEntry);
            return;
        }
        String eType = getEventType(event);
        if(eType.equalsIgnoreCase("TYPE_WINDOW_CONTENT_CHANGED")) return;

        /*
         *If the event came from an new unmonitored package, stop sensorLogger and skip
          */
        String pName = event.getPackageName().toString();
        if(!isMonitored(pName,pkgNames)){
            if(eType.equalsIgnoreCase("TYPE_WINDOW_STATE_CHANGED")){
                this.sensorLogger.stop();
                if(sensorStopped == false) {
                    currentLogEntry = "Switch to unmonitored package " + pName + ", stop sensor logger.";
                    saveLogEntry(debugInfo, currentLogEntry);
                    sensorStopped = true;
                }
            }
            else{
                this.sensorLogger.stop();
                return;
            }
        }

        String cName = event.getClassName().toString();
        long timeStamp = event.getEventTime();
        String eText = getEventText(event);
        int windowId = event.getWindowId();
        int[] indices = getIndex(event,eText);


        Log.v(TAG, String.format(
                "[type] %s [class] %s [package] %s [time] %s [sysTime] %s [text] %s [windowId] %s [fromIndex] %s [toIndex] %s [itemCount] %s ",
                eType, cName, pName, timeStamp, sTimeStamp, eText, windowId, indices[0], indices[1], indices[2]));


        Rect boundsInParent = new Rect();
        Rect boundsInScreen = new Rect();

        String sourceName = source.getClassName().toString();
        source.getBoundsInParent(boundsInParent);
        source.getBoundsInScreen(boundsInScreen);

        String viewResourceId = getViewResourceId(source);
        StringBuilder pBounds = new StringBuilder();
        StringBuilder sBounds = new StringBuilder();
        pBounds.append(String.format("%s, %s, %s, %s", boundsInParent.top, boundsInParent.left, boundsInParent.bottom, boundsInParent.right));
        sBounds.append(String.format("%s, %s, %s, %s", boundsInScreen.top, boundsInScreen.left, boundsInScreen.bottom, boundsInScreen.right));

        Log.v(TAG, String.format("[SourceClass] %s [ViewId] %s [BoundsInParent] (%s, %s, %s, %s) " +
                        "[BoundsInScreen] (%s, %s, %s, %s)",
                sourceName, viewResourceId,
                boundsInParent.top, boundsInParent.left, boundsInParent.bottom, boundsInParent.right,
                boundsInScreen.top, boundsInScreen.left, boundsInScreen.bottom, boundsInScreen.right));

        Log.v(TAG,"--------------parents------------------");
        AccessibilityNodeInfo parent = source.getParent();
        while(parent!=null) {
            Log.v(TAG, "|" + parent.getClassName().toString());
            parent = parent.getParent();
        }
        Log.v(TAG,"--------------parents------------------");

        /**
         * Invoke sensorLogger if the launched activity is in monitored packages.
         */
        if (eType.equalsIgnoreCase("TYPE_WINDOW_STATE_CHANGED")){
            if(isMonitored(pName, pkgNames)){
                this.sensorLogger.start();
                if(sensorStopped == true) {
                    currentLogEntry = "Switch to monitored package, start sensor logger.";
                    saveLogEntry(debugInfo, currentLogEntry);
                    sensorStopped = false;
                }
            }
            else{
                this.sensorLogger.stop();
            }
        }
        String windowInfo = null;
        StringBuilder sb = new StringBuilder();
        if(eType.equalsIgnoreCase("TYPE_VIEW_CLICKED")) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if(rootNode == null){
                currentLogEntry = "Root node info is null.";
                saveLogEntry(debugInfo, currentLogEntry);
                return;
            }
            else{
                recycle(sb, rootNode, 1);
                windowInfo = sb.toString();
            }

        }

        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_EVENT_TYPE , eType);
        values.put(LogEntry.COLUMN_NAME_EVENT_SOURCE, cName);
        values.put(LogEntry.COLUMN_NAME_PKG_NAME, pName);
        values.put(LogEntry.COLUMN_NAME_EVENT_TIME, timeStamp);
        values.put(LogEntry.COLUMN_NAME_SYSTEM_TIME, sTimeStamp);

        String readableTime = Utils.getReadableTime(sTimeStamp);
        values.put(LogEntry.COLUMN_NAME_CALENDAR_TIME, readableTime);
        values.put(LogEntry.COLUMN_NAME_EVENT_TEXT, eText);
        values.put(LogEntry.COLUMN_NAME_WINDOW_ID, windowId);
        values.put(LogEntry.COLUMN_NAME_INDEX_ZERO, indices[0]);
        values.put(LogEntry.COLUMN_NAME_INDEX_ONE, indices[1]);
        values.put(LogEntry.COLUMN_NAME_INDEX_TWO, indices[2]);
        values.put(LogEntry.COLUMN_NAME_SOURCE_CLASS, sourceName);
        values.put(LogEntry.COLUMN_NAME_VIEW_RESOURCE_ID, viewResourceId);
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_PARENT, pBounds.toString());
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_SCREEN, sBounds.toString());
        values.put(LogEntry.COLUMN_NAME_WINDOW_INFO, windowInfo);
        db.insert(LogEntry.EVENT_TABLE_NAME,null,values);

        for (String s : debugInfo){
            writeToDebugDB(sTimeStamp,s);
        }

        source.recycle();
    }

    private void saveLogEntry(ArrayList<String> debug, String curr){
        Log.v(TAG, curr);
        debug.add(curr);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        long sTimeStamp = System.currentTimeMillis();

        LogDbHelper lDbHelper = new LogDbHelper(this);
        db = lDbHelper.getWritableDatabase();

        currentLogEntry = "DB Path: " + db.getPath();
        Log.v(TAG, currentLogEntry);
        writeToDebugDB(sTimeStamp, currentLogEntry);

        currentLogEntry = "onServiceConnected"+" - SDK: " + Build.VERSION.SDK_INT;
        Log.v(TAG, currentLogEntry);
        writeToDebugDB(sTimeStamp, currentLogEntry);

        currentLogEntry = "Current UUID is : " + Utils.getUUID(this);
        Log.v(TAG, currentLogEntry);
        writeToDebugDB(sTimeStamp, currentLogEntry);
        try {
            sensorLogger = new SensorLogger(this);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        startServiceNotification();
    }

    protected String getViewResourceId(AccessibilityNodeInfo info){
        String viewResourceId = "";
        if(Build.VERSION.SDK_INT >=18){
            viewResourceId = info.getViewIdResourceName();
        }
        return viewResourceId;
    }

    protected boolean isMonitored(String pName, String[] pkgNames){
        for(int i=0; i<pkgNames.length;i++){
            if(pName.equalsIgnoreCase(pkgNames[i])) return true;
        }
        return false;
    }

    protected void recycle(StringBuilder sb, AccessibilityNodeInfo info, int level) {
        if (info == null) return;
        String levelInfo = generateLevel(level);
        sb.append(levelInfo);
        sb.append("[ClassName] "+ info.getClassName()+ " [ViewId] "+this.getViewResourceId(info)+
                " [Text] "+ info.getText() + " [WINDOW_ID] "+ info.getWindowId());
        sb.append("\n");
        Log.i(TAG, levelInfo + "[ClassName] " + info.getClassName() + " [ViewId] " + this.getViewResourceId(info) +
                " [Text] " + info.getText() + " [WINDOW_ID] " + info.getWindowId());

        if(info.getChildCount()!=0){
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    recycle(sb, info.getChild(i), level + 1);
                }
            }
        }
    }

    private String generateLevel(int level){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < level; ++i){
            sb.append("--");
        }

        return sb.toString();
    }

    private String getEventType(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                return "TYPE_VIEW_HOVER_ENTER";
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                return "TYPE_VIEW_HOVER_EXIT";
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_START";
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_END";
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                return "TYPE_WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                return "TYPE_VIEW_SCROLLED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                return "TYPE_VIEW_TEXT_SELECTION_CHANGED";
        }
        return "OTHER_EVENT";
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }

    private int[] getIndex(AccessibilityEvent event, String eType){
        int[] index = new int[]{-1, -1, -1};
        switch(eType){
            case "TYPE_VIEW_CLICKED":
            case "TYPE_VIEW_LONG_CLICKED":
            case "TYPE_VIEW_SELECTED":
            case "TYPE_VIEW_FOCUSED":
            case "TYPE_VIEW_TEXT_SELECTION_CHANGED":
            case "TYPE_VIEW_SCROLLED":
                index[0] = event.getFromIndex();
                index[1] = event.getToIndex();
                index[2] = event.getItemCount();
                break;

            case "TYPE_VIEW_TEXT_CHANGE":
                index[0] = event.getFromIndex();
                break;
            case "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY":
                index[0] = event.getFromIndex();
                index[1] = event.getToIndex();
                break;
            default:
                break;
        }
        return index;
    }

    @Override
    public void onInterrupt() {
        long sTimeStamp = System.currentTimeMillis();
        currentLogEntry = "onInterrupt";
        writeToDebugDB(sTimeStamp, currentLogEntry);
        Log.v(TAG, currentLogEntry);
    }

    private void writeToDebugDB(long sysTime, String entry){
        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_SYSTEM_TIME, sysTime);
        String readableTime = Utils.getReadableTime(sysTime);
        values.put(LogEntry.COLUMN_NAME_CALENDAR_TIME, readableTime);
        values.put(LogEntry.COLUMN_NAME_DEBUG_INFO, entry);
        db.insert(LogEntry.DEBUG_INFO_TABLE_NAME, null, values);
    }

    private void writeStartLogToDB(long sysTime){
        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_EVENT_TYPE , "log_start");
        values.put(LogEntry.COLUMN_NAME_EVENT_SOURCE, "");
        values.put(LogEntry.COLUMN_NAME_PKG_NAME, "");
        values.put(LogEntry.COLUMN_NAME_EVENT_TIME, "");
        values.put(LogEntry.COLUMN_NAME_SYSTEM_TIME, sysTime);

        String readableTime = Utils.getReadableTime(sysTime);
        values.put(LogEntry.COLUMN_NAME_CALENDAR_TIME, readableTime);
        values.put(LogEntry.COLUMN_NAME_EVENT_TEXT, "Start Task");
        values.put(LogEntry.COLUMN_NAME_WINDOW_ID, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_ZERO, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_ONE, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_TWO, "");
        values.put(LogEntry.COLUMN_NAME_SOURCE_CLASS, "");
        values.put(LogEntry.COLUMN_NAME_VIEW_RESOURCE_ID, "");
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_PARENT, "");
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_SCREEN, "");
        values.put(LogEntry.COLUMN_NAME_WINDOW_INFO, "");
        db.insert(LogEntry.EVENT_TABLE_NAME,null,values);
    }

    private void writeFinishLogToDB(long sysTime){
        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_EVENT_TYPE , "log_finish");
        values.put(LogEntry.COLUMN_NAME_EVENT_SOURCE, "");
        values.put(LogEntry.COLUMN_NAME_PKG_NAME, "");
        values.put(LogEntry.COLUMN_NAME_EVENT_TIME, "");
        values.put(LogEntry.COLUMN_NAME_SYSTEM_TIME, sysTime);

        String readableTime = Utils.getReadableTime(sysTime);
        values.put(LogEntry.COLUMN_NAME_CALENDAR_TIME, readableTime);
        values.put(LogEntry.COLUMN_NAME_EVENT_TEXT, "Finish Task");
        values.put(LogEntry.COLUMN_NAME_WINDOW_ID, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_ZERO, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_ONE, "");
        values.put(LogEntry.COLUMN_NAME_INDEX_TWO, "");
        values.put(LogEntry.COLUMN_NAME_SOURCE_CLASS, "");
        values.put(LogEntry.COLUMN_NAME_VIEW_RESOURCE_ID, "");
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_PARENT, "");
        values.put(LogEntry.COLUMN_NAME_BOUNDS_IN_SCREEN, "");
        values.put(LogEntry.COLUMN_NAME_WINDOW_INFO, "");
        db.insert(LogEntry.EVENT_TABLE_NAME,null,values);
    }

    private String getPkgNames(String[] names){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < names.length; ++i){
            sb.append(names[i]);
            if(i != names.length-1){
                sb.append("|");
            }
        }
        return sb.toString();
    }

    @Override
    public void onDestroy(){
        if(sensorLogger != null){
            sensorLogger.stop();
        }
        long sTimeStamp = System.currentTimeMillis();
        currentLogEntry = "OnDestroy and stop sensor logger";
        writeToDebugDB(sTimeStamp,currentLogEntry);

        stopForeground(true);
    }

    private void startServiceNotification() {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent startIntent = new Intent("start_task_msg");
        Intent finishIntent = new Intent("finish_task_msg");
        PendingIntent pendingStartIntent = PendingIntent.getBroadcast(this, 0, startIntent, 0);
        PendingIntent pendingFinishIntent = PendingIntent.getBroadcast(this, 0, finishIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.start_button, pendingStartIntent);
        remoteViews.setOnClickPendingIntent(R.id.finish_button, pendingFinishIntent);
//        remoteViews.setTextViewText(R.id.title_message,"Please log task start and finish time.");

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        //PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContent(remoteViews)
                .setColor(Color.parseColor("#ffffffff"));

        Notification notification = builder.build();
//        notification.bigContentView = remoteViews;
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(12345, notification);
    }
}
