package top.cclove.powergesture;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Map;

public class TcpThread extends Thread {
    private static final String TAG = TcpThread.class.getSimpleName();

    private String mHost;
    private int mPort;
    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private Handler mHandler;

    public TcpThread(String server, int port, Handler handler) {
        mHost = server;
        mPort = port;
        mHandler = handler;
    }

    @Override
    public void run() {
        super.run();

        try {
            mSocket = new Socket(mHost, mPort);
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            Message msg = new Message();
            msg.what = MainActivity.MSG_TCP_CONNECTED;
            msg.obj = this;
            mHandler.sendMessage(msg);

            JsonReader jr = new JsonReader(mInputStream) {
                @Override
                void onJsonObjectReceive(JSONObject jsonObject) {
                    Log.d(TAG, "onJsonObjectReceive: " + jsonObject.toString());
                }

                @Override
                void onJsonArrayReceive(JSONArray jsonArray) {
                    Log.d(TAG, "onJsonArrayReceive: " + jsonArray.toString());
                }

                @Override
                public boolean onJsonExceptionOccured(JSONException e) {
                    return super.onJsonExceptionOccured(e);
                }
            };

            jr.beginRead();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendData(int action, Map<String, Float> map) throws JSONException, IOException {
        JSONObject jo = new JSONObject();
        jo.put("action", action);
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            jo.put(entry.getKey(), ((float) entry.getValue()));
        }
        byte[] joBytes = jo.toString().getBytes(Charset.forName("ascii"));
        mOutputStream.write(joBytes);
        mOutputStream.flush();
    }
    public void sendData(String data) throws JSONException, IOException {
        byte[] joBytes = data.toString().getBytes(Charset.forName("ascii"));
        mOutputStream.write(joBytes);
        mOutputStream.flush();
    }

    public void stopConnection(){
        try {
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            mInputStream = null;
            mOutputStream = null;
            mSocket = null;
        }
    }
}
