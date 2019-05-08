package top.cclove.powergesture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;
import static java.lang.Math.signum;

@SuppressLint({"DefaultLocale", "HandlerLeak", "ClickableViewAccessibility"})
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int LOG = 777;
    public static final int MSG_TCP_CONNECTED = 778;

    public static final int MODE_FOLLOW = 1;
    public static final int MODE_ADJUST_WORLD = 2;
    public static final int MODE_ADJUST_SELF = 3;
    public static final int MODE_TRANSLATE = 4;
    public static final int MODE_SCALE = 5;

    private Handler mHandler;
    private TcpThread mTcpThread;
    private ExecutorService mExecutorService;

//    private TextView mTvLog, mTvLog2, mTvLog3;
//    private Button mBtn1,mBtn2,mBtn3, mBtn4;
    private Button mBtn5,mBtn6,mBtn7, mBtn8;
    private ImageView mImBtnFollow, mImBtnAdjust;
    private EditText mEt1, mEt2;
    private FrameLayout mFrame;

    private ImageView mImViewScan;
    private DecoratedBarcodeView mBarcodeView;
    private CaptureManager mCaptureManager;
    private SensorManager mSensorManager;
    private FrameLayout mFrameLayout;

    private PowerGestureSensorListener mListener;
//    private GLSurfaceView mGLSurfaceView;
//    private GlExampleRenderer mRenderer;

    private boolean mIsTurnOn = false;
    private boolean mIsFollowMode = false;
    private boolean mIsAdjustWorldMode = false;
    private boolean mIsAdjustSelfMode = false;
    private boolean mIsTranslateMode = false;
    private boolean mIsScaleMode = false;

//    private int mTestMode = 0;
//    private int TESTNUM = 2;

    private Random mRandom = new Random();

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsTurnOn) {
            registerSensorListeners();
        }
//        if (mGLSurfaceView!=null) // when launch app, skip
//            mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListeners();
//        mGLSurfaceView.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
//        mTvLog = findViewById(R.id.tv_log);
//        mTvLog2 = findViewById(R.id.tv_log_2);
//        mTvLog3 = findViewById(R.id.tv_log_3);
//        mBtn1 = findViewById(R.id.btn_1);
//        mBtn2 = findViewById(R.id.btn_2);
//        mBtn3 = findViewById(R.id.btn_3);
//        mBtn4 = findViewById(R.id.btn_4);
//        mBtn5 = findViewById(R.id.btn_5);
//        mBtn6 = findViewById(R.id.btn_6);
        mBtn7 = findViewById(R.id.btn_7);
        mBtn8 = findViewById(R.id.btn_8);
//        mEt1 = findViewById(R.id.et_1);
//        mEt2 = findViewById(R.id.et_2);
//        mFrame = findViewById(R.id.frame);
        mImBtnFollow = findViewById(R.id.calibrate);
        mImBtnAdjust = findViewById(R.id.freeze);
        mBarcodeView = findViewById(R.id.barcode);
        mFrameLayout = findViewById(R.id.container);
        mImViewScan = findViewById(R.id.scanner);

        mImViewScan.setOnClickListener(v -> {
            mBarcodeView = new DecoratedBarcodeView(MainActivity.this);
            mFrameLayout.addView(mBarcodeView);
            mCaptureManager = new CaptureManager(this, mBarcodeView);
            mCaptureManager.initializeFromIntent(getIntent(), savedInstanceState);
            mCaptureManager.onResume();
            mCaptureManager.decode();
            mBarcodeView.decodeSingle(new MyBarcodeCallback());
        });

        mCaptureManager = new CaptureManager(this, mBarcodeView);
        mCaptureManager.initializeFromIntent(getIntent(), savedInstanceState);
        mCaptureManager.onResume();
        mCaptureManager.decode();

        mBarcodeView.decodeSingle(new MyBarcodeCallback());

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case LOG:{
//                        showLog(mTvLog3, msg.obj.toString());
                        Log.d(TAG, "handleMessage: " + msg.obj.toString());
                    }
                    case MSG_TCP_CONNECTED:{
                        mTcpThread = (TcpThread) msg.obj;
                        registerSensorListeners();
                    }
                }
            }
        };
        mSensorManager = ((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensorList) {
            Log.d(TAG, "onCreate: " + s.getName());
        }

        // init gl example renderer
//        mRenderer = new GlExampleRenderer();

        // init listener
        mListener = new PowerGestureSensorListener();

        // init thread pool
        mExecutorService = Executors.newSingleThreadExecutor();

//        mBtn1.setOnClickListener(v->{
//            registerSensorListeners();
//            mIsTurnOn = true;
//        });
//        mBtn2.setOnClickListener(v->{
//            unregisterSensorListeners();
//            mIsTurnOn = false;
//        });
        mImBtnFollow.setOnTouchListener((v, event) -> {
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    mIsFollowMode = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mIsFollowMode = false;
                    break;
            }
            return true;
        });
        mImBtnAdjust.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsAdjustWorldMode = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mIsAdjustWorldMode = false;
                    mListener.clearGyroscopeData();
                    break;
            }
            return true;
        });
//        mBtn5.setOnClickListener(e -> {
//            mTcpThread = new TcpThread(mEt1.getText().toString(), Integer.parseInt(mEt2.getText().toString()));
//            mTcpThread.start();
//        });
        mBtn7.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsTranslateMode = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mIsTranslateMode = false;
                    mListener.clearLAData();
                    break;
            }
            return true;
        });
        mBtn8.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsScaleMode = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mIsScaleMode = false;
                    mListener.clearLAData();
                    break;
            }
            return true;
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
//        mGLSurfaceView = new GLSurfaceView(this);
//        mGLSurfaceView.setRenderer(mRenderer);
//        mGLSurfaceView.setZOrderOnTop(true);
//        mFrame.addView(mGLSurfaceView);
    }

    // register listener
    private void registerSensorListeners() {
//            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
//            mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
//            mSensorManager.registerListener(mRenderer, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 10000);
    }

    // unregister listener
    private void unregisterSensorListeners() {
        mSensorManager.unregisterListener(mListener);
    }

    void showLog(TextView tv, String msg) {
        tv.setText(String.format("[%s] %s", getCurrentTime(), msg));
    }

    void sendTcp(String data) {
        Log.d(TAG, "sendTcp: " + (mTcpThread == null));
        mExecutorService.submit(() -> {
            try {
                mTcpThread.sendData(data);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    static String getCurrentTime(){
        return sdf.format(new Date());
    }

    //region GL
//    class GlExampleRenderer implements GLSurfaceView.Renderer {
//        private Cube mCube;
//        public final float[] mRotationMatrix = new float[16];
//        public final double[] mGyroscopeData = new double[3];
//        double mYAcc = 0;
//
//        public final double[] mTranslateData = new double[3];
//
//        public GlExampleRenderer() {
//            mCube = new Cube();
//            // initialize the rotation matrix to identity
//            mRotationMatrix[0] = 1;
//            mRotationMatrix[4] = 1;
//            mRotationMatrix[8] = 1;
//            mRotationMatrix[12] = 1;
//        }
//
//        public void onDrawFrame(GL10 gl) {
//
//            // clear screen
//            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
//            // set-up modelview matrix
//            gl.glMatrixMode(GL10.GL_MODELVIEW);
//            if (mIsFollowMode) {
//                gl.glPopMatrix(); // delete tge previous matrix
//                gl.glLoadIdentity();
//                gl.glTranslatef(0, 0, -3.0f);
////                gl.glRotatef(mGyroscopeData[0]*60, 1, 0, 0);
////                gl.glRotatef(mGyroscopeData[1]*60, 0, 1, 0);
////                gl.glRotatef(mGyroscopeData[2]*60, 0, 0, 1);
//                gl.glMultMatrixf(mRotationMatrix, 0);
//                gl.glPushMatrix();
//            } else if (mIsAdjustWorldMode) {
//                gl.glPopMatrix(); // 用个卵子库，今天老子要硬算
//                float[] matrixYRotate = new float[16];
//                float c = (float) cos(mGyroscopeData[1]*20), s = (float) sin(mGyroscopeData[1]*20);
//                mYAcc += mGyroscopeData[1] * 20;
//                Log.d(TAG, "onDrawFrame: " + mYAcc);
//                if (mGyroscopeData[1]<0){
//                    gl.glPushMatrix();
//                }else {
//                    matrixYRotate[0] = c;
//                    matrixYRotate[2] = s;
//                    matrixYRotate[5] = 1;
//                    matrixYRotate[8] = -s;
//                    matrixYRotate[10] = c;
//                    matrixYRotate[15] = 1;
//                    gl.glLoadIdentity();
//                    gl.glTranslatef(0, 0, -3);
//                    gl.glMultMatrixf(matrixYRotate, 0);
//                    gl.glMultMatrixf(mRotationMatrix, 0);
//                    gl.glPushMatrix();
//                }
///*
//                Message msg = new Message();
//                msg.what = LOG;
//                msg.obj = String.format("[%.2f][%.2f][%.2f][%.2f]\n[%.2f][%.2f][%.2f][%.2f]\n" +
//                                "[%.2f][%.2f][%.2f][%.2f]\n[%.2f][%.2f][%.2f][%.2f]",
//                        m[0], m[1], m[2], m[3],
//                        m[4], m[5], m[6], m[7],
//                        m[8], m[9], m[10], m[11],
//                        m[12], m[13], m[14], m[15]
//                );
//                mHandler.sendMessage(msg);
//*/
////                gl.glRotatef(mGyroscopeData[0]*60, 1, 0, 0);
////                gl.glRotatef(mGyroscopeData[1] * 60, 0, 1, 0);
////                gl.glRotatef(mGyroscopeData[2]*60, 0, 0, 1);
//            } else if (mIsTranslateMode) {
//
//            }
//            // draw our object
//            gl.glPopMatrix();
//            gl.glPushMatrix();
//            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
//            mCube.draw(gl);
//        }
//
//        public void onSurfaceChanged(GL10 gl, int width, int height) {
//            // set view-port
//            gl.glViewport(0, 0, width, height);
//            // set projection matrix
//            float ratio = (float) width / height;
//            gl.glMatrixMode(GL10.GL_PROJECTION);
//            gl.glLoadIdentity();
//            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
//            Log.d(TAG, "onSurfaceChanged: " + width + height);
//        }
//
//        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//            // dither is enabled by default, we don't need it
//            gl.glDisable(GL10.GL_DITHER);
//            // clear screen in white
//            gl.glClearColor(1, 1, 1, 1);
//            gl.glLoadIdentity();
//            gl.glTranslatef(0, 0, -3.0f);
//            gl.glPushMatrix();
//        }
//
//        class Cube {
//            // initialize our cube
//            private FloatBuffer mVertexBuffer;
//            private FloatBuffer mColorBuffer;
//            private ByteBuffer mIndexBuffer;
//
//            public Cube() {
//                final float vertices[] = {
//                        -1, -1, -1, 1, -1, -1,
//                        1, 1, -1, -1, 1, -1,
//                        -1, -1, 1, 1, -1, 1,
//                        1, 1, 1, -1, 1, 1,
//                };
//                final float colors[] = {
//                        0, 0, 0, 1, 1, 0, 0, 1,
//                        1, 1, 0, 1, 0, 1, 0, 1,
//                        0, 0, 1, 1, 1, 0, 1, 1,
//                        1, 1, 1, 1, 0, 1, 1, 1,
//                };
//                final byte indices[] = {
//                        0, 4, 5, 0, 5, 1,
//                        1, 5, 6, 1, 6, 2,
//                        2, 6, 7, 2, 7, 3,
//                        3, 7, 4, 3, 4, 0,
//                        4, 7, 6, 4, 6, 5,
//                        3, 0, 1, 3, 1, 2
//                };
//                ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
//                vbb.order(ByteOrder.nativeOrder());
//                mVertexBuffer = vbb.asFloatBuffer();
//                mVertexBuffer.put(vertices);
//                mVertexBuffer.position(0);
//                ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
//                cbb.order(ByteOrder.nativeOrder());
//                mColorBuffer = cbb.asFloatBuffer();
//                mColorBuffer.put(colors);
//                mColorBuffer.position(0);
//                mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
//                mIndexBuffer.put(indices);
//                mIndexBuffer.position(0);
//            }
//
//            public void draw(GL10 gl) {
//                gl.glEnable(GL10.GL_CULL_FACE);
//                gl.glFrontFace(GL10.GL_CW);
//                gl.glShadeModel(GL10.GL_SMOOTH);
//                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
//                gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
//                gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
//            }
//        }
//    }
    //endregion

    class PowerGestureSensorListener implements SensorEventListener {
//        private GlExampleRenderer mGlExampleRender;

        private static final double NS2S = 1e-9;

        private float mGyroscopeLastX=0;
        private float mGyroscopeLastY=0, mGyroscopeLastZ = 0;
        private long mGyroscopeLastReportTime = 0;

        private double mLALastVX = 0, mLALastVY = 0;

        public void clearGyroscopeData() {
//            mGyroscopeXAcc = 0;
//            mGyroscopeYAcc = 0;
//            mGyroscopeZAcc = 0;
            mGyroscopeLastX = 0;
            mGyroscopeLastY = 0;
            mGyroscopeLastZ = 0;
            mGyroscopeLastReportTime = 0;
        }
        public void clearLAData() {
            mLALastVX = 0; mLALastVY = 0;
        }

// region PGSL GL
//        public PowerGestureSensorListener(GlExampleRenderer exampleRenderer) {
//            this.mGlExampleRender = exampleRenderer;
//        }
// endregion

        public PowerGestureSensorListener(){
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mIsFollowMode) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR || event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    float X = event.values[0];
                    float Y = event.values[1];
                    float Z = event.values[2];
                    float W = event.values[3];
//                    SensorManager.getRotationMatrixFromVector(mGlExampleRender.mRotationMatrix, event.values);
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("a", MODE_FOLLOW);
                        jo.put("X", float2str(X));
                        jo.put("Y", float2str(Y));
                        jo.put("Z", float2str(Z));
                        jo.put("W", float2str(W));
                        sendTcp(jo.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mIsAdjustSelfMode) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float X = event.values[0];
                    float Y = event.values[1];
                    float Z = event.values[2];
                    long currentTime = event.timestamp;
                    if (mGyroscopeLastReportTime == 0) {
                        mGyroscopeLastReportTime = currentTime;
                        mGyroscopeLastX = event.values[0];
                        mGyroscopeLastY = event.values[1];
                        mGyroscopeLastZ = event.values[2];
                        return;
                    }
                    long duration_ns = currentTime - mGyroscopeLastReportTime;
                    mGyroscopeLastReportTime = currentTime;
                    double duration_s = duration_ns * NS2S;
                    double XIncrement = (X + mGyroscopeLastX) * duration_s / 2;
                    double YIncrement = (Y + mGyroscopeLastY) * duration_s / 2;
                    double ZIncrement = (Z + mGyroscopeLastZ) * duration_s / 2;
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("a", MODE_ADJUST_SELF);
                        jo.put("X", float2str(rad2deg(XIncrement)));
                        jo.put("Y", float2str(rad2deg(YIncrement)));
                        jo.put("Z", float2str(rad2deg(ZIncrement)));
                        sendTcp(jo.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mIsAdjustWorldMode) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float X = event.values[0];
                    float Y = event.values[1];
                    float Z = event.values[2];
                    long currentTime = event.timestamp;
                    if (mGyroscopeLastReportTime == 0) {
                        mGyroscopeLastReportTime = currentTime;
                        mGyroscopeLastX = event.values[0];
                        mGyroscopeLastY = event.values[1];
                        mGyroscopeLastZ = event.values[2];
                        return;
                    }
                    long duration_ns = currentTime - mGyroscopeLastReportTime;
                    mGyroscopeLastReportTime = currentTime;
                    double duration_s = duration_ns * NS2S;
                    double XIncrement = (X + mGyroscopeLastX) * duration_s / 2;
                    double YIncrement = (Y + mGyroscopeLastY) * duration_s / 2;
                    double ZIncrement = (Z + mGyroscopeLastZ) * duration_s / 2;
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("a", MODE_ADJUST_WORLD);
                        jo.put("X", float2str(rad2deg(XIncrement)));
                        jo.put("Y", float2str(rad2deg(YIncrement)));
                        jo.put("Z", float2str(rad2deg(ZIncrement)));
                        sendTcp(jo.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mIsTranslateMode) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    float X = event.values[0];
                    float Y = event.values[1];
                    mLALastVX += X;
                    mLALastVY += Y;

                    try {
                        JSONObject jo = new JSONObject();
                        jo.put("a", MODE_TRANSLATE);
                        jo.put("X", float2str(mLALastVX));
                        jo.put("Y", float2str(mLALastVY));
                        sendTcp(jo.toString());
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

//                    // 抑制
//                    if (abs(mLALastVX) > 5 * abs(mLALastVY)) {
//                        mLALastVY = 0;
//                    } else if (abs(mLALastVX) < abs(mLALastVY) / 5) {
//                        mLALastVX = 0;
//                    }
//                    double directionX = mLALastVX > 0.5 ? 1 : (mLALastVX < -0.5 ? -1 : 0);
//                    double directionY = mLALastVY > 0.5 ? 1 : (mLALastVY < -0.5 ? -1 : 0);
//                    if (directionX != 0 || directionY != 0) {
//                        JSONObject jo = new JSONObject();
//                        try {
//                            jo.put("a", MODE_TRANSLATE);
//                            jo.put("X", float2str(directionX * 0.1));
//                            jo.put("Y", float2str(directionY * 0.1));
//                            sendTcp(jo.toString());
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }
            } else if (mIsScaleMode) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    float Y = event.values[1];
                    mLALastVY += Y;
                    double directionY = mLALastVY > 0.5 ? 1 : (mLALastVY < -0.5 ? -1 : 0);
                    if (directionY != 0) {
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("a", MODE_SCALE);
                            jo.put("Y", float2str(directionY * 0.1));
                            sendTcp(jo.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
//region
/*
            do {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    showLog(mTvLog3, String.format("\nX: %.2f\nY: %.2f\nZ: %.2f", X_lateral, Y_longitudinal, Z_vertical));
                } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    String log ="";
                    float X = event.values[0];
                    float Y = event.values[1];
                    float Z = event.values[2];
                    log += String.format("陀螺仪当前速度\nX: %.2f\nY: %.2f\nZ: %.2f\n", X, Y, Z);
                    if (!mIsAdjustWorldMode) {
                        showLog(mTvLog2, log);
                        break;
                    }
                    long currentTime = event.timestamp;
                    if (mGyroscopeLastReportTime == 0) {
                        mGyroscopeLastReportTime = currentTime;
                        mGyroscopeLastX = event.values[0];
                        mGyroscopeLastY = event.values[1];
                        mGyroscopeLastZ = event.values[2];
                        showLog(mTvLog2, log);
                        break;
                    }
                    long duration_ns = currentTime - mGyroscopeLastReportTime;
                    mGyroscopeLastReportTime = currentTime;
                    double duration_s = duration_ns * NS2S;

                    double XIncrement = (X + mGyroscopeLastX) * duration_s / 2;
                    double YIncrement = (Y + mGyroscopeLastX) * duration_s / 2;
                    double ZIncrement = (Z + mGyroscopeLastX) * duration_s / 2;
//                    double XIncrement = 0; // 测试锁死
//                    double YIncrement = 0; // 测试锁死
//                    double ZIncrement = 0; // 测试锁死
                    mGyroscopeXAcc += XIncrement;
                    mGyroscopeYAcc += YIncrement;
                    mGyroscopeZAcc += ZIncrement;
                    mGlExampleRender.mGyroscopeData[0] = XIncrement;
                    mGlExampleRender.mGyroscopeData[1] = YIncrement;
                    mGlExampleRender.mGyroscopeData[2] = ZIncrement;
//                    mGlExampleRender.mGyroscopeData[0] = (float) mGyroscopeXAcc;
//                    mGlExampleRender.mGyroscopeData[1] = (float) mGyroscopeYAcc;
//                    mGlExampleRender.mGyroscopeData[2] = (float) mGyroscopeZAcc;
                    mGyroscopeLastX = X;
                    mGyroscopeLastY = Y;
                    mGyroscopeLastZ = Z;
                    log += String.format("增量:[%.2f][%.2f][%.2f]累计[%.2f] duration_ns:[%d]",
                            XIncrement, YIncrement, ZIncrement, mGyroscopeYAcc, duration_ns);
                    showLog(mTvLog2, log);

                    if (mIsAdjustWorldMode) {
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("a", MODE_ADJUST_WORLD);
                            jo.put("X", float2str(rad2deg(XIncrement)));
                            jo.put("Y", float2str(rad2deg(YIncrement)));
                            jo.put("Z", float2str(rad2deg(ZIncrement)));
                            sendTcp(jo.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (mIsAdjustSelfMode) {
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("a", MODE_ADJUST_SELF);
                            jo.put("X", float2str(rad2deg(XIncrement)));
                            jo.put("Y", float2str(rad2deg(YIncrement)));
                            jo.put("Z", float2str(rad2deg(ZIncrement)));
                            sendTcp(jo.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    float X = event.values[0];
                    float Y = event.values[1];
//                    float Z = event.values[2];
                    if (mIsTranslateMode) {
                        long currentTime = event.timestamp;
                        if (mLALastReportTime == 0) {
                            mLALastReportTime = currentTime;
                            break;
                        }
                        long duration_ns = currentTime - mLALastReportTime;
                        mLALastReportTime = currentTime;
                        double duration_s = duration_ns * NS2S;
                        mLALastVX += X * duration_s;
                        double dx = mLALastVX / 2 * duration_s;
                        Log.d(TAG, String.format("onSensorChanged: AX[%.2f] VX[%.2f] DX[%.2f]", X, mLALastVX, dx));
                    }

                } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    float X = event.values[0];
                    float Y = event.values[1];
                    float Z = event.values[2];
                    float W = event.values[3];
                    float ACC = event.values[4];
                    showLog(mTvLog, String.format("旋转矢量传感器\nX: %.2f\nY: %.2f\n" +
                            "Z: %.2f\nW: %.2f\nA: %.2f", X, Y, Z, W, ACC));

                    if (mIsFollowMode) {
                        SensorManager.getRotationMatrixFromVector(mGlExampleRender.mRotationMatrix, event.values);

                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("a", MODE_FOLLOW);
                            jo.put("X", float2str(X));
                            jo.put("Y", float2str(Y));
                            jo.put("Z", float2str(Z));
                            jo.put("W", float2str(W));
                            sendTcp(jo.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                    float X_lateral = event.values[0];
                    float Y_longitudinal = event.values[1];
                    float Z_vertical = event.values[2];
                    showLog(mTvLog3, String.format("方向传感器\nX: %.2f\nY: %.2f\nZ: %.2f",
                            X_lateral, Y_longitudinal, Z_vertical));
                }
            } while (false);
*/
//endregion
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    class MyBarcodeCallback implements BarcodeCallback {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result != null){
                try {
                    JSONObject jo = new JSONObject(result.getText());
                    JSONArray ja = jo.getJSONArray("i");
                    for (int i = 0; i < ja.length(); i++) {
                        String ip = ja.getString(i);

                        TcpThread tcpThread = new TcpThread(ip, 8052, mHandler);
                        tcpThread.start();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                Log.e(getClass().getName(), "获取到的扫描结果是：" + result.getText());
                mCaptureManager.onDestroy();
                TextView textView = new TextView(MainActivity.this);
                textView.setTextColor(Color.WHITE);
                textView.setText(result.getText());
                mFrameLayout.removeAllViews();
                mFrameLayout.addView(textView);
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    }

    static float rad2deg(float r) {
        return r * 180 / 3.1415926f;
    }
    static float rad2deg(double r) {
        return (float) (r * 180 / 3.1415926);
    }
    static String float2str(float f) {
        return String.format("%.2f", f);
    }
    static String float2str(double d) {
        return String.format("%.2f", d);
    }


    private void checkPermission() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

}
