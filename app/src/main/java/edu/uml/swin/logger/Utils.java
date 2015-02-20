package edu.uml.swin.logger;

import android.content.Context;
import android.provider.Settings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm");
        return getReadableTime(formatter, sysTime);
    }

    public static String getCurrentTime(){
        long sysTime = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        return getReadableTime(formatter, sysTime);
    }

    public static String getUUID(Context context) {
        String uuid, serialNo, androidId;

        serialNo = android.os.Build.SERIAL;
        androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        uuid = serialNo + "-" + androidId;
        // MD5 hash the device ID
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(uuid.getBytes());
            byte msgBytes[] = messageDigest.digest();
            for (int i = 0; i < msgBytes.length; i++)
                hexString.append(Integer.toHexString(0xFF & msgBytes[i]));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hexString.toString();
    }
}
