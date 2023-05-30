// HTTP GET.

package com.dialectek.nimbus;

import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPget {

    private String mURLname;
    public HttpURLConnection httpConn;
    public Exception exception;

    public HTTPget(String URLname) {
        mURLname = URLname;
        httpConn = null;
        exception = null;
    }

    public int get() {
        int status = -1;
        try {
            URL url = new URL(mURLname);
            httpConn = (HttpURLConnection) url.openConnection();
            status = httpConn.getResponseCode();
        } catch (Exception e) {
            exception = e;
        }
        return status;
    }

    public void close() {
        if (httpConn != null) {
            httpConn.disconnect();
        }
    }
}
