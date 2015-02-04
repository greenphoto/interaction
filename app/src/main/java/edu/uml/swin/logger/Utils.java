package edu.uml.swin.logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by admin on 2/4/15.
 */
public class Utils {

    public static String getReadableTime(long currentMillis){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);
        return formatter.format(calendar.getTime());
    }
}
