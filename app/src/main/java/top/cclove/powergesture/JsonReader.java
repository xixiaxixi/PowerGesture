package top.cclove.powergesture;

import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public abstract class JsonReader {
    private final static int TYPE_BRACE = 0;
    private final static int TYPE_SQUARE = 1;
    private final static int TYPE_WAIT = -1;
    private final static int MAX_RESET_COUNT = 4;


    abstract void onJsonObjectReceive(JSONObject jsonObject);

    abstract void onJsonArrayReceive(JSONArray jsonArray);

    /**
     *
     * @param e
     * @return Default return false. If false, JsonReader stops and throws. If true, JsonReader resets and goes on.
     */
    public boolean onJsonExceptionOccured(JSONException e){return false;};

    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    public JsonReader(InputStream inputStream, String charset) throws UnsupportedEncodingException {
        this.inputStream = inputStream;
        if (!Charset.isSupported(charset)) {
            throw new UnsupportedEncodingException(charset);
        } else {
            this.charset = charset;
        }
    }

    public JsonReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public JsonReader(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public JsonReader(InputStream inputStream, OutputStream outputStream, String charset) throws UnsupportedEncodingException {
        this(inputStream, charset);
        this.outputStream = outputStream;
    }

    private String charset = "UTF-8";

    private ArrayList<Integer> stack = new ArrayList<Integer>();
    private int pair = 0;//括号对
    private int type = TYPE_WAIT;//起始括号类型
    private boolean in_bracket = false;//是否在引号内
    private boolean is_trans = false;//前一个字符是否为转义\
    private int reset_count = 0;//重置计数器 // 好吧这是我的协议

    void beginRead() throws IOException, JSONException {

        int c = 0;
        try {
            while (true) {
                c = inputStream.read();
                System.out.println("get:　" + c);

                if (c == 0) {
                    if (++reset_count == MAX_RESET_COUNT) {
                        reset();
                    }
                }else {
                    reset_count = 0;
                }

                if (c == -1) {
                    throw new IOException("Client shut down the connection");
                }
                if (type == TYPE_WAIT) {
                    switch (((char) c)) {
                        case '{':
                            type = TYPE_BRACE;
                            pair++;
                            stack.add(c);
                            continue;
                        case '[':
                            type = TYPE_SQUARE;
                            pair++;
                            stack.add(c);
                            continue;
                        default:
                            continue;
                    }
                } else if (type == TYPE_BRACE && !in_bracket) {
                    if (((char) c) == '{') {
                        pair++;
                    } else if (((char) c) == '}') {
                        pair--;
                    } else if (((char) c) == '"') {
                        in_bracket = true;
                    }
                    stack.add(c);
                } else if (type == TYPE_SQUARE && !in_bracket){
                    if (((char) c) == '[') {
                        pair++;
                    } else if (((char) c) == ']') {
                        pair--;
                    } else if (((char) c) == '"') {
                        in_bracket = true;
                    }
                    stack.add(c);
                }else {
                    if (is_trans) {
                        is_trans = false;
                        stack.add(c);
                    } else {
                        switch (((char) c)) {
                            case '"':
                                in_bracket = false;
                                stack.add(c);
                                break;
                            case '\\':
                                is_trans = true;
                                stack.add(c);
                                break;
                            default:
                                stack.add(c);
                                break;
                        }
                    }
                }
                if (pair == 0) {
                    if (type == TYPE_BRACE) {
//                        int[] str = new int[stack.size()];
//                        for (int i = 0; i < stack.size(); i++) {
//                            str[i] = ((int) stack.get(i));
//                        }
//                        JSONObject jo = new JSONObject(new String(str, 0, stack.size()));
                        byte[] str = new byte[stack.size()];
                        for (int i = 0; i < stack.size(); i++) {
                            str[i] = (byte) ((int) stack.get(i));
                        }
                        JSONObject jo = new JSONObject(new String(str, 0, stack.size(), charset));
                        onJsonObjectReceive(jo);
                        stack.clear();
                        type = -1;
                    } else {
                        int[] str = new int[stack.size()];
                        for (int i = 0; i < stack.size(); i++) {
                            str[i] = ((int) stack.get(i));
                        }
                        JSONArray ja = new JSONArray(new String(str, 0, stack.size()));
                        onJsonArrayReceive(ja);
                        stack.clear();
                        type = TYPE_WAIT;
                    }
                }
            }

        } catch (IOException e) {
            throw e;
        } catch (JSONException ex) {
            boolean b = onJsonExceptionOccured(ex);
            if (b) {
                reset();
            } else {
                throw ex;
            }
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void reset() {
        pair = 0;
        type = TYPE_WAIT;
        in_bracket = false;
        is_trans = false;
        stack.clear();
    }


}
