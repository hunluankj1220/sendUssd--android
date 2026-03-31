package com.project.setussd.utils.chato;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.hardware.display.DisplayManager;

import java.nio.ByteBuffer;

public class ScreenCaptureManager {

    private static ImageReader reader;
    private static int width, height;
    private static boolean isInitialized = false;

    public static void init(MediaProjection mp, int w, int h, int dpi) {
        if (isInitialized) {
            release();
        }

        width = w;
        height = h;

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);

        mp.createVirtualDisplay(
                "screen",
                w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null, null
        );

        isInitialized = true;
    }

    public static Bitmap capture() {
        if (!isInitialized || reader == null) return null;

        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) return null;

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();

            int rowStride = planes[0].getRowStride();
            int pixelStride = planes[0].getPixelStride();

            int bitmapWidth = width + (rowStride - pixelStride * width) / pixelStride;

            Bitmap bmp = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buffer);

            Bitmap result = Bitmap.createBitmap(bmp, 0, 0, width, height);
            bmp.recycle();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    public static void release() {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        isInitialized = false;
    }
}
