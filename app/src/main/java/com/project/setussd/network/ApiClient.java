package com.project.setussd.network;


import java.util.Map;

/**
 * 请求类
 */
public class ApiClient {

    private static final String BASE_URL = "https://你的接口地址";

    // Activity传参直接透传
    public static <T> void request(
            String path,
            Map<String, String> params,
            Class<T> clazz,
            ApiCallback<T> callback
    ) {
        OkHttpManager.getInstance().post(
                BASE_URL + path,
                params,
                clazz,
                callback
        );
    }
}