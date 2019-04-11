package top.cclove.powergesture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int LOG = 777;

    Handler mHandler;
    TextView mTvLog, mTvLog2, mTvLog3;
    Button mBtn1,mBtn2;
    SensorManager mSensorManager;
    SensorEventListener mListener;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvLog = findViewById(R.id.tv_log);
        mTvLog2 = findViewById(R.id.tv_log_2);
        mTvLog3 = findViewById(R.id.tv_log_3);
        mBtn1 = findViewById(R.id.btn_1);
        mBtn2 = findViewById(R.id.btn_2);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case LOG:{
                        showLog(mTvLog, msg.obj.toString());
                    }
                }
            }
        };
        mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    showLog(mTvLog3, String.format("\nX: %.2f\nY: %.2f\nZ: %.2f", X_lateral, Y_longitudinal, Z_vertical));
                } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    showLog(mTvLog2, String.format("线性加速度传感器\nX: %.2f\nY: %.2f\nZ: %.2f", X_lateral, Y_longitudinal, Z_vertical));
                } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    showLog(mTvLog3, String.format("陀螺仪\nX: %.2f\nY: %.2f\nZ: %.2f", X_lateral, Y_longitudinal, Z_vertical));
                } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    float W = event.values[3];
                    float ACC = event.values[4];
                    showLog(mTvLog, String.format("旋转矢量传感器\nX: %.2f\nY: %.2f\nZ: %.2f\nW: %.2f\nA: %.2f", X_lateral, Y_longitudinal, Z_vertical, W, ACC));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager = ((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensorList) {
            Log.d(TAG, "onCreate: " + s.getName());
        }

        mBtn1.setOnClickListener(v->{
//            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        });
        mBtn2.setOnClickListener(v->{
            mSensorManager.unregisterListener(mListener);
        });

    }

    void showLog(TextView tv, String msg) {
        tv.setText(String.format("[%s] %s", getCurrentTime(), msg));
    }


    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    static String getCurrentTime(){
        return sdf.format(new Date());
    }
}
