package com.project.setussd.log;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ussd执行日志
 */
public class UssdLogger {

    public static final String ACTION_LOG = "ussd_log";
    public static final String EXTRA_LOG = "log";

    public static void log(Context context, String msg) {

        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());

        String log = time + "  " + msg;

        Log.i("USSD_LOG", log);

        Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(EXTRA_LOG, log);

        context.sendBroadcast(intent);
    }
}