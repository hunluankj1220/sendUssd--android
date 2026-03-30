package com.project.setussd.network;

/**
 * 请求回调接口
 * @param <T>
 */
public interface ApiCallback<T> {

    // 请求成功 + 业务成功
    void onSuccess(T data);

    // 请求成功，但业务失败（比如 code != 200）
    void onError(int code, String msg);

    // 网络错误（无网络/超时等）
    void onFailure(String error);
}