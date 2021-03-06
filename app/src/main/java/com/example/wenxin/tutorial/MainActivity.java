package com.example.wenxin.tutorial;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.*;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements SensorEventListener2 {

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Thread sendThread;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationReading = new float[3];

    private long intervalInMilliseconds = 100;
    private long previousScheduledTime = 0;

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    TextView rotX, rotY, rotZ;
    Button leftClick, rightClick, calibrationButton;
    String str;

    private boolean leftClickPressed = false;
    private boolean rightClickPressed = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        rotX = (TextView) findViewById(R.id.textView);
        rotY = (TextView) findViewById(R.id.textView2);
        rotZ = (TextView) findViewById(R.id.textView3);
        leftClick = (Button) findViewById(R.id.btnLC);
        rightClick = (Button) findViewById(R.id.btnRC);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        leftClick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    leftClickPressed = true;
                }else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                    leftClickPressed = false;
                }
                //Log.d("On Touch Left Click", MotionEvent.actionToString(event.getAction()));
                return true;
            }
        });

        rightClick.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    rightClickPressed = true;
                }else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_DOWN){
                    rightClickPressed = false;
                }
                //Log.d("On Touch Right Click", Boolean.toString(rightClickPressed));
                return true;
            }
        });


//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //TODO:Figure out what to do on accuracy change
    }

    @Override
    protected void onResume(){
        super.onResume();
        this.mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        this.mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        this.mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause(){
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
//            Log.d("X Accelerometer",Float.toString(event.values[0]));
//            Log.d("Y Accelerometer", Float.toString(event.values[1]));
//            Log.d("Z Accelerometer", Float.toString(event.values[2]));
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
//            Log.d("X Magnetometer",Float.toString(event.values[0]));
//            Log.d("Y Magnetometer",Float.toString(event.values[1]));
//            Log.d("Z Magnetometer",Float.toString(event.values[2]));
        }else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(event.values, 0, mRotationReading, 0, mRotationReading.length);
        }

//        Log.d("X Orientation", Float.toString(mOrientationAngles[0]));
//        Log.d("Y Orientation", Float.toString(mOrientationAngles[1]));
//        Log.d("Z Orientation", Float.toString(mOrientationAngles[2]));
        updateOrientationAngles();

        String rotXStr = Float.toString(mRotationReading[2]);
        String rotYStr = Float.toString(mOrientationAngles[1]);
        String rotZStr = Float.toString(mOrientationAngles[2]);

        rotX.setText(rotXStr);
        rotY.setText(rotYStr);
        rotZ.setText(rotZStr);

        SendDataTask task =  new SendDataTask();

        float leftClickConversion = 0f;
        float rightClickConversion = 0f;

        if(leftClickPressed){
            leftClickConversion = 1f;
        }

        if(rightClickPressed){
            rightClickConversion = 1f;
        }

        //Log.d("Left Click",Float.toString(leftClickConversion));
        //Log.d("Right Click",Float.toString(rightClickConversion));
        task.execute(mRotationReading[2],mOrientationAngles[1],mOrientationAngles[2],leftClickConversion,rightClickConversion);
    }

    public void updateOrientationAngles(){
        mSensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
        //TODO:What do I do in this method?
    }


}
