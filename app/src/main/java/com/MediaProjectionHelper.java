package com;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class MediaProjectionHelper {

    private final Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenDensity;
    private int width, height;

    public static final int REQUEST_MEDIA_PROJECTION = 1001;

    public MediaProjectionHelper(Context context) {
        this.context = context;

        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;
        width = metrics.widthPixels;
        height = metrics.heightPixels;
    }

    public Intent createScreenCaptureIntent() {
        return projectionManager.createScreenCaptureIntent();
    }

    public void setMediaProjection(MediaProjection projection) {
        this.mediaProjection = projection;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    public void capturePixelColor(Rect bounds, PixelColorCallback callback) {
        HandlerThread handlerThread = new HandlerThread("PixelCapture");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.postDelayed(() -> {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);

                image.close();

                int centerX = bounds.centerX();
                int centerY = bounds.centerY();
                if (centerX < bitmap.getWidth() && centerY < bitmap.getHeight()) {
                    int color = bitmap.getPixel(centerX, centerY);
                    callback.onColorCaptured(color);
                } else {
                    callback.onColorCaptured(-1);
                }
            } else {
                callback.onColorCaptured(-1);
            }

            handlerThread.quitSafely();
        }, 400);
    }

    public interface PixelColorCallback {
        void onColorCaptured(int color);
    }
}
