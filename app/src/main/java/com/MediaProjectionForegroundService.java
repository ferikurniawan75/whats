package com;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjectionManager;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MediaProjectionForegroundService extends Service {

    public static final String CHANNEL_ID = "MediaProjectionChannel";
    public static final int NOTIF_ID = 1002;
    public static final String EXTRA_MEDIA_PROJECTION = "extra_projection";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen capture active")
                .setContentText("Capturing WhatsApp bubble")
                .build();
        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent dataIntent = intent.getParcelableExtra("dataIntent");

        if (resultCode == -1 || dataIntent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection projection = manager.getMediaProjection(resultCode, dataIntent);

        if (projection != null) {
            MediaProjectionHelper helper = new MediaProjectionHelper(getApplicationContext());
            helper.setMediaProjection(projection);
            MediaProjectionProvider.set(helper);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "MediaProjection Logger",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
