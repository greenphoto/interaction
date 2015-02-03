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
public class SensorLogger extends Task {
    private static final String TAG = "InteractionService/SensorLogger";
    private static final String APP_DIR = "AccessibilityData";
    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private SensorEventListener sensorListener;
    File accOutput;
    File gyroOutput;
    PrintWriter accPw;
    PrintWriter gyroPw;
    File appDir;

    public SensorLogger(Context ctx) throws IOException {
        super(ctx);
        sensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
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
        sensorManager.registerListener(sensorListener,senGyroscope,SensorManager.SENSOR_DELAY_FASTEST);
        long timestamp = System.currentTimeMillis();
        String accFileName = "acc" + timestamp;
        String gyroFileName = "gyro" + timestamp;
        accOutput = new File(appDir,accFileName);
        gyroOutput = new File(appDir, gyroFileName);
        if(accOutput.exists()){
            Log.v(TAG, "acc file exists.");
        }
        else{
            Log.v(TAG, "acc file does not exist and to be created.");
        }
        if(gyroOutput.exists()){
            Log.v(TAG, "gyro file exists.");
        }
        else{
            Log.v(TAG, "gyro file does not exist and to be created.");
        }
        try {
            accPw = new PrintWriter(new FileOutputStream(accOutput));
            gyroPw = new PrintWriter(new FileOutputStream(gyroOutput));
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
            accPw.close();
            gyroPw.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
