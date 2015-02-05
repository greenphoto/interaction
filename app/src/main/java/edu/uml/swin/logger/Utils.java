package edu.uml.swin.logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by admin on 2/4/15.
 */
public class Utils {
    private static SimpleDateFormat defaultFormatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    public static String getReadableTime(long currentMillis){
        SimpleDateFormat formatter = defaultFormatter;
        return getReadableTime(formatter, currentMillis);
    }

    public static String getReadableTime(SimpleDateFormat formatter, long currentMillis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);
        return formatter.format(calendar.getTime());
    }

    public static String getTimeAsFileName(){
        long sysTime = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("MM_DD_hh_mm");
        return getReadableTime(formatter, sysTime);
    }
}
