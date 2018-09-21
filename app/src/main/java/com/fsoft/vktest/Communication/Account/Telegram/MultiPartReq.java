package com.fsoft.vktest.Communication.Account.Telegram;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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
import com.android.volley.Request;
import com.android.volley.Response;
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
            entity.writeTo(bos);
            String entityContentAsString = new String(bos.toByteArray());
            Log.e("volley", entityContentAsString);
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected void deliverResponse(NetworkResponse arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));

    }

    private void buildMultipartEntity() {
        entity.addPart(mStringPart, new FileBody(mFilePart));
        try {
            for (String key : parameters.keySet())
                entity.addPart(key, new StringBody(parameters.get(key), Charset.forName("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            VolleyLog.e("UnsupportedEncodingException");
        }
    }
}
