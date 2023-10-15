package com.maple.audiometry.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

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
		double volume = 20 * Math.log10(mean);

		if (volume > 100) {
			sendDangerousNotification();
		} else if (volume >= 90) {
			sendPotentiallyDangerousNotification();
		}

		return volume * 1.05;
	}

	private void sendPotentiallyDangerousNotification() {
		sendNotification("Potentially Dangerous Sound Alert", "Detected sound level between 90dB to 100dB!");
	}

	private void sendDangerousNotification() {
		sendNotification("Dangerous Sound Alert", "Detected sound level over 100dB! Should be avoided.");
	}

	private void sendNotification(String title, String content) {
		String channelId = "dangerous_sound_alert";
		String channelName = "Dangerous Sound Alert Channel";

		NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(channel);
		}

		Notification notification = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			notification = new Notification.Builder(mContext, channelId)
					.setContentTitle(title)
					.setContentText(content)
					.setSmallIcon(R.drawable.icon_512) // replace with your icon
					.build();
		}

		notificationManager.notify(1, notification);
	}
}