package com.project.setussd.utils.chato;


import android.graphics.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.hardware.display.DisplayManager;

import java.nio.ByteBuffer;

/**
 * 截图
 */
public class ScreenCaptureManager {

    private static ImageReader reader;
    private static int width, height;

    public static void init(MediaProjection mp, int w, int h, int dpi) {

        width = w;
        height = h;

        reader = ImageReader.newInstance(w, h,
                PixelFormat.RGBA_8888, 2);

        mp.createVirtualDisplay(
                "screen",
                w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null, null
        );
    }

    public static Bitmap capture() {

        try {
            Image image = reader.acquireLatestImage();
            if (image == null) return null;

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();

            int rowStride = planes[0].getRowStride();
            int pixelStride = planes[0].getPixelStride();

            Bitmap bmp = Bitmap.createBitmap(
                    width + (rowStride - pixelStride * width) / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
            );

            bmp.copyPixelsFromBuffer(buffer);
            image.close();

            return Bitmap.createBitmap(bmp, 0, 0, width, height);

        } catch (Exception e) {
            return null;
        }
    }
}