package com.maple.audiometry.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.net.Uri;

import com.maple.audiometry.R;

public class AudioRecordBasedDemo {

	private static final String TAG = "AudioRecordBasedDemo";
	private static final int SAMPLE_RATE_IN_HZ = 8000;
	private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
			SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
			AudioFormat.ENCODING_PCM_16BIT);
	private final NoiseValueUpdateCallback mNoiseCallBack;
	private AudioRecord mAudioRecord;
	private boolean isRecording;
	private Context mContext;

	public interface NoiseValueUpdateCallback {
		void onUpdateNoiseValue(double noiseValue);
	}

	public AudioRecordBasedDemo(NoiseValueUpdateCallback noiseValueUpdateCallback, Context context) {
		this.mNoiseCallBack = noiseValueUpdateCallback;
		this.mContext = context;
	}

	public void startRecord() {
		if (mAudioRecord == null) {
			mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
					AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
		}
		isRecording = true;
		mAudioRecord.startRecording();

		new Thread(new Runnable() {
			@Override
			public void run() {
				short[] buffer = new short[BUFFER_SIZE];
				while (isRecording) {
					double volume = getVolume(buffer);
					mNoiseCallBack.onUpdateNoiseValue(volume);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				mAudioRecord.stop();
				mAudioRecord.release();
				mAudioRecord = null;
			}
		}).start();
	}

	public void stopRecord() {
		isRecording = false;
	}

	private double getVolume(short[] buffer) {
		int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
		long v = 0;
		for (int i = 0; i < buffer.length; i++) {
			v += buffer[i] * buffer[i];
		}
		double mean = v / (double) r;
		double volume = 0;

		// Only compute decibels if mean is greater than 1 to avoid negative or -Infinity dB values
		if (mean > 1) {
			volume = 12.6 * Math.log10(mean);
		}

		if (volume > 100) {
			sendDangerousNotification();
		} else if (volume >= 90) {
			sendPotentiallyDangerousNotification();
		}

		return volume * 1;
	}

	private void sendPotentiallyDangerousNotification() {
		sendNotification("Potentially Dangerous Sound Alert",
				"Detected sound level between 90dB to 100dB!",
				R.raw.sound_potentially_dangerous,
				"potentially_dangerous_sound_alert");  // Use a different channel ID
	}

	private void sendDangerousNotification() {
		sendNotification("Dangerous Sound Alert",
				"Detected sound level over 100dB! Should be avoided.",
				R.raw.sound_dangerous,
				"dangerous_sound_alert");
	}

	private void sendNotification(String title, String content, int soundResId, String channelId) {
		String channelName = title;  // Use the title as channel name for simplicity

		NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
			if (channel == null) {
				channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);

				Uri soundUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + soundResId);
				AudioAttributes audioAttributes = new AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setUsage(AudioAttributes.USAGE_NOTIFICATION)
						.build();

				channel.setSound(soundUri, audioAttributes);
				notificationManager.createNotificationChannel(channel);
			}
		}

		Notification notification = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			notification = new Notification.Builder(mContext, channelId)
					.setContentTitle(title)
					.setContentText(content)
					.setSmallIcon(R.drawable.icon_512)
					.build();
		}

		notificationManager.notify(1, notification);
	}


}