package com.maple.audiometry.utils;

import java.io.File;
import java.io.IOException;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.maple.audiometry.R;

public class MediaRecorderDemo {
	private final String TAG = "MediaRecord";
	public static final int MAX_LENGTH = 1000 * 60 * 10;
	private MediaRecorder mMediaRecorder;
	private int BASE = 1;
	private int SPACE = 100;
	private long startTime;
	private long endTime;
	private String filePath;
	private NoiseValueUpdateCallback mNoiseCallBack;
	private final Handler mHandler = new Handler();
	private Context mContext; // <-- Added this line for context

	private Runnable mUpdateMicStatusTimer = new Runnable() {
		public void run() {
			updateMicStatus();
		}
	};

	public interface NoiseValueUpdateCallback {
		void onUpdateNoiseValue(double noiseValue);
	}

	public MediaRecorderDemo(Context context, NoiseValueUpdateCallback noiseValueUpdateCallback) {
		this.filePath = "/dev/null";
		this.mNoiseCallBack = noiseValueUpdateCallback;
		this.mContext = context; // <-- Initialized the context here
	}

	public MediaRecorderDemo(Context context, File file, NoiseValueUpdateCallback noiseValueUpdateCallback) {
		this.filePath = file.getAbsolutePath();
		this.mNoiseCallBack = noiseValueUpdateCallback;
		this.mContext = context; // <-- Initialized the context here
	}

	public void startRecord() {
		if (mMediaRecorder == null)
			mMediaRecorder = new MediaRecorder();
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
			Log.i("ACTION_START", "开始时间" + startTime);
		} catch (IllegalStateException | IOException e) {
			Log.i(TAG, "call startAmr(File mRecAudioFile) 失败!" + e.getMessage());
		}
	}

	private void updateMicStatus() {
		if (mMediaRecorder != null) {
			double ratio = (double) mMediaRecorder.getMaxAmplitude() / BASE;
			double db = 0;
			if (ratio > 1)
				db = 20 * Math.log10(ratio);

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
			NotificationChannel channel = new NotificationChannel("noise_alert_channel", "Noise Alerts", NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(channel);
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "noise_alert_channel")
				.setSmallIcon(R.drawable.icon_512)
				.setContentTitle("High Noise Level Detected!")
				.setContentText("Noise level over 80 dB & WhatTheHack2 is the BEST!")
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		notificationManager.notify(1, builder.build());
	}

	public long stopRecord() {
		if (mMediaRecorder == null)
			return 0L;
		endTime = System.currentTimeMillis();
		Log.i("ACTION_END", "结束时间" + endTime);
		mMediaRecorder.stop();
		mMediaRecorder.reset();
		mMediaRecorder.release();
		mMediaRecorder = null;
		Log.i("ACTION_LENGTH", "时间" + (endTime - startTime));
		return endTime - startTime;
	}
}
