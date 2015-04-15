package edu.uml.swin.logger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by jing on 1/19/15.
 */
public class SensorLogger extends Task {
    private static final String TAG = "InteractionService/SensorLogger";
    protected static final String APP_DIR = "InteractionLoggerSensorData";
    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private SensorEventListener sensorListener;
    private LogDbHelper dbHelper;
    private DBUtil dbWriter;
    private String currentLogEntry = "";
    private File accOutput;
    private File gyroOutput;
    private PrintWriter accPw;
    private PrintWriter gyroPw;
    private File appDir;
    protected static final String accFileName = "acc"; //+ timestamp;
    protected static final String gyroFileName = "gyro";// + timestamp;

    public SensorLogger(Context ctx) throws IOException {
        super(ctx);
        sensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        dbHelper = new LogDbHelper(ctx);
        dbWriter = new DBUtil(dbHelper);

        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                long t = System.currentTimeMillis();

                Sensor mySensor = sensorEvent.sensor;

                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];
//                    Log.v(TAG,String.format("(%s, %s, %s, %s)", x, y, z, t));
                    StringBuilder sb = new StringBuilder();
                    sb.append(x+", ");
                    sb.append(y+", ");
                    sb.append(z+", ");
                    sb.append(t+"\n");
                    try {
                        accPw.print(sb.toString());
//                        outputStream.flush();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if (mySensor.getType() == Sensor.TYPE_GYROSCOPE){
                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];
//                    Log.v(TAG,String.format("(%s, %s, %s, %s)", x, y, z, t));
                    StringBuilder sb = new StringBuilder();
                    sb.append(x+", ");
                    sb.append(y+", ");
                    sb.append(z+", ");
                    sb.append(t+"\n");
                    try {
                        gyroPw.print(sb.toString());
//                        outputStream.flush();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        long sTime = System.currentTimeMillis();
        ArrayList<String> debugInfo = new ArrayList<String>();

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            currentLogEntry = "External storage is writable.";
            Log.v(TAG, currentLogEntry);
            debugInfo.add(currentLogEntry);

            appDir = Environment.getExternalStoragePublicDirectory(APP_DIR);
            if(!appDir.exists()){
                if(!appDir.mkdir()){
                    currentLogEntry = "Sensor data directory failed to create.";
                    saveLogEntry(debugInfo, currentLogEntry);

                    throw new IOException(currentLogEntry);
                }
            }
            else{
                currentLogEntry = "Sensor data directory already existed or succeeded to create.";
                saveLogEntry(debugInfo, currentLogEntry);
            }
        }
        else{
            currentLogEntry = "External storage is not writable.";
            saveLogEntry(debugInfo, currentLogEntry);

        }

        accOutput = new File(appDir,accFileName);
        gyroOutput = new File(appDir, gyroFileName);
        if(accOutput.exists()){
            currentLogEntry = "Acc data file already exists.";
            saveLogEntry(debugInfo, currentLogEntry);
        }
        else{
            currentLogEntry = "Acc data file does not exist and will be created.";
            saveLogEntry(debugInfo, currentLogEntry);
        }
        if(gyroOutput.exists()){
            currentLogEntry = "Gyro data file already exists.";
            saveLogEntry(debugInfo, currentLogEntry);
        }
        else{
            currentLogEntry = "Gyro data file does not exist and will be created.";
            saveLogEntry(debugInfo, currentLogEntry);
        }

        for (String s: debugInfo){
            dbWriter.writeToDebugDB(sTime, s);
        }

//        try {
//            accPw = new PrintWriter(new FileOutputStream(accOutput, true));
//            gyroPw = new PrintWriter(new FileOutputStream(gyroOutput, true));
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
    }

    private void saveLogEntry(ArrayList<String> debug, String curr){
        Log.v(TAG, curr);
        debug.add(curr);
    }

    @Override
    protected void onStart()  {
        long timestamp = System.currentTimeMillis();
        currentLogEntry = "Sensor logger on start.";
        dbWriter.writeToDebugDB(timestamp, currentLogEntry);

        sensorManager.registerListener(sensorListener,senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorListener,senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        try {
            accPw = new PrintWriter(new FileOutputStream(accOutput, true));
            gyroPw = new PrintWriter(new FileOutputStream(gyroOutput, true));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        long timestamp = System.currentTimeMillis();
        currentLogEntry = "Sensor logger on stop.";

        Log.d(TAG, currentLogEntry);
        dbWriter.writeToDebugDB(timestamp, currentLogEntry);

        sensorManager.unregisterListener(sensorListener);
        try {
            accPw.flush();
            accPw.close();
            gyroPw.flush();
            gyroPw.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
