package com;

public class MediaProjectionProvider {
    private static MediaProjectionHelper instance;

    public static void set(MediaProjectionHelper helper) {
        instance = helper;
    }

    public static MediaProjectionHelper get() {
        return instance;
    }
}
