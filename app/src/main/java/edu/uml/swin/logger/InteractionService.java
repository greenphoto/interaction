package edu.uml.swin.logger;


import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;

public class InteractionService extends AccessibilityService {

    static final String TAG = "InteractionService";
    private AccLogger accLogger;
    private String[] pkgNames = {"com.skcc.corfire.dd","com.example.android.notepad","com.android.launcher"};


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
        return "DEFAULT_EVENT";
    }

    private String getEventText(AccessibilityEvent event) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence s : event.getText()) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        if(getEventType(event).equalsIgnoreCase("Default")) return;
        AccessibilityNodeInfo source = event.getSource();
        if(source == null){
            Log.v(TAG, "Failed to get source.");
            return;
        }
        else{
            Log.v(TAG, String.format("[SourceClass] %s [ViewId] %s",
                    source.getClassName().toString(), source.getViewIdResourceName()));
            Log.v(TAG,"--------------parents------------------");
            AccessibilityNodeInfo parent = source.getParent();
            while(parent!=null) {
                Log.v(TAG, parent.getClassName().toString());
                parent = parent.getParent();
            }
            Log.v(TAG,"--------------parents------------------");
        }

        String eType = getEventType(event);
        String cName = event.getClassName().toString();
        String pName = event.getPackageName().toString();
        long timeStamp = event.getEventTime();
        String eText = getEventText(event);
        int windowId = event.getWindowId();

        if(getEventType(event).equalsIgnoreCase("TYPE_WINDOW_CONTENT_CHANGED")) return;

        Log.v(TAG, String.format(
                "[type] %s [class] %s [package] %s [time] %s [text] %s [windowId] %s",
                eType, cName, pName, timeStamp, eText, windowId));
        /**
         * Invoke AccLogger if the launched activity is in monitored packages.
         */
        if (eType.equalsIgnoreCase("TYPE_WINDOW_STATE_CHANGED")){
            if(isMonitored(pName, pkgNames)){
                this.accLogger.start();
            }
            else{
                this.accLogger.stop();
            }
        }


        if(eType.equalsIgnoreCase("TYPE_VIEW_CLICKED")) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if(rootNode == null){
                Log.v(TAG,"Root node info is null.");
                return;
            }
            else{
                recycle(rootNode, 1);
            }
            Log.v(TAG,"============================================");
        }
        source.recycle();
    }

    protected boolean isMonitored(String pName, String[] pkgNames){
        for(int i=0; i<pkgNames.length;i++){
            if(pName.equalsIgnoreCase(pkgNames[i])) return true;
        }
        return false;
    }

    protected void recycle(AccessibilityNodeInfo info, int level) {
        String levelInfo = generateLevel(level);
        Log.i(TAG, levelInfo+ "[ClassName] "+ info.getClassName()+" [Text] "+ info.getText() + " [WINDOW_ID] "+ info.getWindowId());

        if(info.getChildCount()!=0){
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    recycle(info.getChild(i), level + 1);
                }
            }
        }
    }

    private String generateLevel(int level){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < level; ++i){
            sb.append(">--");
        }

        return sb.toString();
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v(TAG, "onServiceConnected");
        try {
            accLogger = new AccLogger(this);
        }
        catch (Exception e){
            e.printStackTrace();
        }

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
        if(accLogger != null){
            accLogger.stop();
        }
    }
}
