package com.housingclient.util.bg;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class BgHttp {

    static final class Response {
        final int code;
        final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body == null ? "" : body;
        }

        boolean ok() {
            return code >= 200 && code < 300;
        }
    }

    private BgHttp() {
    }

    static Response request(String method, String urlStr, String body, Map<String, String> headers)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(BgConfig.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(BgConfig.READ_TIMEOUT_MS);
        conn.setUseCaches(false);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("apikey", BgConfig.KEY);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    conn.setRequestProperty(e.getKey(), e.getValue());
                }
            }
        }

        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            OutputStream os = conn.getOutputStream();
            try {
                os.write(bytes);
            } finally {
                os.close();
            }
        }

        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String respBody = readFully(in);
        conn.disconnect();
        return new Response(code, respBody);
    }

    private static String readFully(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;
        try {
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
                if (bos.size() > 256 * 1024) {
                    break;
                }
            }
        } finally {
            in.close();
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
