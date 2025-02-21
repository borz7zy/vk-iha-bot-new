package com.fsoft.vktest.Communication.Account.Telegram;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.HttpHeaderParser;

public class MultiPartReq extends Request < NetworkResponse > {
    private Response.Listener<String> mListener = null;
    private Response.ErrorListener mEListener;
    //
    private final File mFilePart;
    private final String mStringPart;
    private Map<String, String> parameters;
    private Map<String, String> header;
    MultipartEntity entity = new MultipartEntity();

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return header;
    }

    public MultiPartReq(String url, Response.ErrorListener eListener,
                        Response.Listener<String> rListener, File file, String stringPart,
                        Map<String, String> param, Map<String, String> head) {
        super(Method.POST, url, eListener);
        mListener = rListener;
        mEListener = eListener;
        mFilePart = file;
        mStringPart = stringPart;
        parameters = param;
        header = head;
        buildMultipartEntity();
    }

    @Override
    public String getBodyContentType() {
        return entity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            Log.d("volley", ". Sending data to server...");
            entity.writeTo(bos);
        } catch (IOException e){
            e.printStackTrace();
            VolleyLog.e("IOException writing to ByteArrayOutputStream: " + e.getMessage());
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success( response, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        try {
            String string = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            mListener.onResponse(string);
        }
        catch (Exception e){
            e.printStackTrace();
            mListener.onResponse("");
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        mEListener.onErrorResponse(error);
    }

    private void buildMultipartEntity() {
        entity.addPart(mStringPart, new FileBody(mFilePart));
        try {
            for (String key : parameters.keySet()) {
                entity.addPart(key, new StringBody(parameters.get(key), StandardCharsets.UTF_8));
            }
        } catch (UnsupportedEncodingException e) {
            VolleyLog.e("UnsupportedEncodingException");
        }
    }
}
