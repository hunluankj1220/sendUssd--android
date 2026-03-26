package com.project.setussd;

import android.app.Application;
import android.content.Context;

/**
 * @项目名称:
 * @包 名： com.project.senussd$
 * @类 名:  MyApplication$
 * @描 述:  java类作用描述
 * @作 者： wusiliang
 * @时 间： 2025/7/30$ 16:45$
 * @版 本：
 * @版 权:  宝利信通(北京)软件股份有限公司版权所有
 */
public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }
    public static Context getContext(){
          return MyApplication.context;
    }
}
