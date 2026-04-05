package com.project.setussd.network;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.project.setussd.bean.ErrorCode;
import com.project.setussd.log.LogInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

/**
 * 请求封装
 */
public class OkHttpManager {

    private static OkHttpManager instance;
    private OkHttpClient client;
    private Handler handler;
    private Gson gson;

    private OkHttpManager() {
        handler = new Handler(Looper.getMainLooper());
        gson = new Gson();

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new LogInterceptor(true))
                .build();
    }

    public static OkHttpManager getInstance() {
        if (instance == null) {
            synchronized (OkHttpManager.class) {
                if (instance == null) {
                    instance = new OkHttpManager();
                }
            }
        }
        return instance;
    }

    // ================= GET =================
    public <T> void get(String url, Map<String, String> params,
                        Class<T> clazz, ApiCallback<T> callback) {

        HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();

        if (params != null) {
            for (String key : params.keySet()) {
                builder.addQueryParameter(key, params.get(key));
            }
        }

        Request request = new Request.Builder()
                .url(builder.build())
                .build();

        execute(request, clazz, callback);
    }

    // ================= POST =================
    public <T> void post(String url, Map<String, String> params,
                         Class<T> clazz, ApiCallback<T> callback) {

        FormBody.Builder body = new FormBody.Builder();

        if (params != null) {
            for (String key : params.keySet()) {
                body.add(key, params.get(key));
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(body.build())
                .build();

        execute(request, clazz, callback);
    }

    // ================= 核心执行 =================
    private <T> void execute(Request request, Class<T> clazz, ApiCallback<T> callback) {

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() == null) {
                    handler.post(() -> callback.onFailure("响应为空"));
                    return;
                }

                String json = response.body().string();

                try {
                    ApiResponse<T> apiResponse =
                            gson.fromJson(json,
                                    com.google.gson.reflect.TypeToken
                                            .getParameterized(ApiResponse.class, clazz)
                                            .getType());

                    handler.post(() -> handleResponse(apiResponse, callback));

                } catch (Exception e) {
                    handler.post(() -> callback.onFailure("解析错误"));
                }
            }
        });
    }

    // ================= 统一处理错误码 =================
    private <T> void handleResponse(ApiResponse<T> resp, ApiCallback<T> callback) {
        if (resp.code == ErrorCode.OK) {
            callback.onSuccess(resp.data);
        } else {
            callback.onError(resp.code, resp.msg);
        }
    }
}