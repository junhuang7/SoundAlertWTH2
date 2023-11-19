package com.maple.audiometry.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.maple.audiometry.R;

import java.io.File;
import java.io.IOException;

public class MediaRecorderDemo {
	private static final String TAG = "MediaRecord";
	private static final int MAX_LENGTH = 1000 * 60 * 10;
	private static final String CHANNEL_ID = "noise_alert_channel";
	private static final String CHANNEL_NAME = "Noise Alerts";

	private MediaRecorder mMediaRecorder;
	private final int BASE = 1;
	private final int SPACE = 100;
	private long startTime;
	private long endTime;
	private final String filePath;
	private final NoiseValueUpdateCallback mNoiseCallBack;
	private final Handler mHandler = new Handler();
	private final Context mContext;

	private final Runnable mUpdateMicStatusTimer = this::updateMicStatus;

	public interface NoiseValueUpdateCallback {
		void onUpdateNoiseValue(double noiseValue);
	}

	public MediaRecorderDemo(Context context, NoiseValueUpdateCallback noiseValueUpdateCallback) {
		this(context, new File("/dev/null"), noiseValueUpdateCallback);
	}

	public MediaRecorderDemo(Context context, File file, NoiseValueUpdateCallback noiseValueUpdateCallback) {
		this.filePath = file.getAbsolutePath();
		this.mNoiseCallBack = noiseValueUpdateCallback;
		this.mContext = context;
	}

	public void startRecord() {
		if (mMediaRecorder == null) {
			mMediaRecorder = new MediaRecorder();
		}
		try {
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mMediaRecorder.setOutputFile(filePath);
			mMediaRecorder.setMaxDuration(MAX_LENGTH);
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			startTime = System.currentTimeMillis();
			updateMicStatus();
			Log.i(TAG, "Start time: " + startTime);
		} catch (IllegalStateException | IOException e) {
			Log.i(TAG, "Failed to start recording: " + e.getMessage());
		}
	}

	private void updateMicStatus() {
		if (mMediaRecorder != null) {
			double ratio = (double) mMediaRecorder.getMaxAmplitude() / BASE;
			double db = 0;
			if (ratio > 1) {
				db = 10 * Math.log10(ratio);
			}

			if (db > 70) {
				showWarningNotification();
			}

			mNoiseCallBack.onUpdateNoiseValue(db);
			mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
		}
	}

	private void showWarningNotification() {
		NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(channel);
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
				.setSmallIcon(R.drawable.icon_512)
				.setContentTitle("High Noise Level Detected!")
				.setContentText("Noise level over 70 dB & WhatTheHack2 is the BEST!")
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		notificationManager.notify(1, builder.build());
	}

	public long stopRecord() {
		if (mMediaRecorder == null) {
			return 0L;
		}
		endTime = System.currentTimeMillis();
		Log.i(TAG, "End time: " + endTime);
		mMediaRecorder.stop();
		mMediaRecorder.reset();
		mMediaRecorder.release();
		mMediaRecorder = null;
		Log.i(TAG, "Duration: " + (endTime - startTime));
		return endTime - startTime;
	}
}
