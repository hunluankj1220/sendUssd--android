package com.project.setussd.service.chato;


import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.os.IBinder;
import android.view.*;
import android.widget.*;

public class FloatWindowService extends Service {

    private static ImageView iv;
    private static TextView tv;

    public static void show(Service s, Bitmap bmp, String text) {

        if (iv != null) {
            iv.setImageBitmap(bmp);
            tv.setText(text);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        iv = new ImageView(this);
        tv = new TextView(this);

        layout.addView(iv);
        layout.addView(tv);

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                600, 600,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        wm.addView(layout, p);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}