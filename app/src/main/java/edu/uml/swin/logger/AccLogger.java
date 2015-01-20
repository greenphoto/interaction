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

/**
 * Created by jing on 1/19/15.
 */
public class AccLogger extends Task {
    private static final String TAG = "AccLogger";
    private static final String APP_DIR = "AccessibilityData";
    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private SensorEventListener sensorListener;
    File outputFile;
    PrintWriter pw;
    File appDir;

    public AccLogger(Context ctx) throws IOException {
        super(ctx);
        sensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Sensor mySensor = sensorEvent.sensor;

                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];
                    long t = System.currentTimeMillis();
//                    Log.v(TAG,String.format("(%s, %s, %s, %s)", x, y, z, t));
                    StringBuilder sb = new StringBuilder();
                    sb.append(x+", ");
                    sb.append(y+", ");
                    sb.append(z+", ");
                    sb.append(t+"\n");
                    try {
                        pw.print(sb.toString());
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
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            Log.v(TAG, "External storage is writable.");
            appDir = Environment.getExternalStoragePublicDirectory(APP_DIR);
            if(!appDir.exists()){
                if(!appDir.mkdir()){
                    Log.v(TAG, "Directory not created");
                    throw new IOException("Directory not created");
                }
            }
            else{
                Log.v(TAG, "Directory existed or created.");
            }
        }
        else{
            Log.v(TAG, "External storage is not writable.");
            throw new IOException("External storage is not writable.");
        }

    }


    @Override
    protected void onStart()  {
        Log.d(TAG, "onStart");
        sensorManager.registerListener(sensorListener,senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        outputFile = new File(appDir,"acc");
        if(outputFile.exists()){
            Log.v(TAG, "acc File exists.");
        }
        else{
            Log.v(TAG, "acc File does not exist.");
        }
        try {
            pw = new PrintWriter(new FileOutputStream(outputFile));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        sensorManager.unregisterListener(sensorListener);
        try {
            pw.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
