package com.project.setussd.service.chato;


import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class CaptureService extends Service {

    public static final String CHANNEL_ID = "capture_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "ScreenCapture",
                            NotificationManager.IMPORTANCE_LOW
                    );

            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);

            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setContentTitle("USSD运行中")
                            .setContentText("正在处理USSD请求")
                            .setSmallIcon(android.R.drawable.ic_menu_camera)
                            .build();

            startForeground(1, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}