package com;

import android.media.projection.MediaProjection;

public class MediaProjectionHolder {
    private static MediaProjection projection;

    public static void set(MediaProjection proj) {
        projection = proj;
    }

    public static MediaProjection get() {
        return projection;
    }
}
