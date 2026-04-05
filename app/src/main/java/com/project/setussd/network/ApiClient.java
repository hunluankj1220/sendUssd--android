package com.project.setussd.network;


import com.project.setussd.Contact;
import com.project.setussd.activity.chato.ChatOneActivity;

import java.util.Map;

/**
 * 请求类
 */
public class ApiClient {


    // Activity传参直接透传
    public static <T> void request(
            String path,
            Map<String, String> params,
            Class<T> clazz,
            ApiCallback<T> callback
    ) {
        OkHttpManager.getInstance().post(
                Contact.serverURL + path,
                params,
                clazz,
                callback
        );
    }

    // Activity传参直接透传
    public static <T> void request2(
            String path,
            Map<String, String> params,
            Class<T> clazz,
            ApiCallback<T> callback
    ) {
        OkHttpManager.getInstance().get(
                Contact.serverURL + path,
                params,
                clazz,
                callback
        );
    }
}