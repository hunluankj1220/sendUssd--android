package com.project.setussd.log;


import android.util.Log;

import java.io.IOException;

import okhttp3.*;
import okio.Buffer;

/**
 * 请求日志
 */
public class LogInterceptor implements Interceptor {

    private static final String TAG = "OkHttpLog";

    // 是否打印日志（上线关闭）
    private boolean isDebug = true;

    public LogInterceptor(boolean isDebug) {
        this.isDebug = isDebug;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        if (!isDebug) {
            return chain.proceed(request);
        }

        long startTime = System.currentTimeMillis();

        // ===== 请求信息 =====
        Log.e(TAG, "======== 请求开始 ========");
        Log.e(TAG, "URL: " + request.url());
        Log.e(TAG, "Method: " + request.method());

        // Header
        Headers headers = request.headers();
        Log.e(TAG, "Headers: " + headers.toString());

        // 请求参数（POST）
        RequestBody requestBody = request.body();
        if (requestBody != null) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            Log.e(TAG, "Params: " + buffer.readUtf8());
        }

        // ===== 执行请求 =====
        Response response = chain.proceed(request);

        long endTime = System.currentTimeMillis();

        // ===== 响应信息 =====
        ResponseBody responseBody = response.body();
        String resp = "";

        if (responseBody != null) {
            resp = responseBody.string();
        }

        Log.e(TAG, "-------- 响应返回 --------");
        Log.e(TAG, "Code: " + response.code());
        Log.e(TAG, "Response: " + resp);
        Log.e(TAG, "耗时: " + (endTime - startTime) + "ms");
        Log.e(TAG, "======== 请求结束 ========");

        // ⚠️ 注意：response.body().string() 只能读一次，需要重建
        return response.newBuilder()
                .body(ResponseBody.create(resp, responseBody.contentType()))
                .build();
    }
}