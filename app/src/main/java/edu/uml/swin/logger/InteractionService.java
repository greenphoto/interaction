package edu.uml.swin.logger;


import android.accessibilityservice.AccessibilityService;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import edu.uml.swin.logger.LogContract.LogEntry;


public class InteractionService extends AccessibilityService {

    static final String TAG = "InteractionService";
    private SensorLogger sensorLogger;
    private SQLiteDatabase db;
    private String[] pkgNames = {"com.skcc.corfire.dd", "org.tasks", "com.example.android.notepad"};

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long sTimeStamp = System.currentTimeMillis();

//        if(getEventType(event).equalsIgnoreCase("Default")) return;
        AccessibilityNodeInfo source = event.getSource();
        if(source == null){
            Log.v(TAG, "Failed to get source.");
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
                Log.v(TAG,"Root node info is null.");
                return;
            }
            else{
                recycle(sb, rootNode, 1);
                windowInfo = sb.toString();
            }
            Log.v(TAG,"============================================");
        }
        ContentValues values = new ContentValues();
        values.put(LogEntry.COLUMN_NAME_EVENT_TYPE , eType);
        values.put(LogEntry.COLUMN_NAME_EVENT_SOURCE, cName);
        values.put(LogEntry.COLUMN_NAME_PKG_NAME, pName);
        values.put(LogEntry.COLUMN_NAME_EVENT_TIME, timeStamp);
        values.put(LogEntry.COLUMN_NAME_SYSTEM_TIME, sTimeStamp);
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

        long newRowId;
        newRowId = db.insert(LogEntry.EVENT_TABLE_NAME,null,values);

        source.recycle();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected"+" - SDK: " + Build.VERSION.SDK_INT);
        try {
            sensorLogger = new SensorLogger(this);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        LogDbHelper lDbHelper = new LogDbHelper(this);
        db = lDbHelper.getWritableDatabase();
        Log.v(TAG, db.getPath());

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
        Log.v(TAG, "onInterrupt");
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
    }
}
