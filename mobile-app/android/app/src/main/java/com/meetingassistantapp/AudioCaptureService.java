package com.meetingassistantapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Base64;

/**
 * 前台服务用于音频捕获
 * Android 14+要求MediaProjection必须在前台服务中运行
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioCaptureService extends Service {

    private static final String CHANNEL_ID = "AudioCaptureChannel";
    private static final int NOTIFICATION_ID = 1001;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private AudioDataCallback callback;

    private final IBinder binder = new LocalBinder();

    public interface AudioDataCallback {
        void onAudioData(String base64Audio, int length);
    }

    public class LocalBinder extends Binder {
        AudioCaptureService getService() {
            return AudioCaptureService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动前台服务
        Notification notification = createNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setMediaProjection(MediaProjection projection) {
        this.mediaProjection = projection;
    }

    public void createMediaProjection(int resultCode, Intent data) {
        this.mediaProjection = projectionManager.getMediaProjection(resultCode, data);
    }

    public void setAudioDataCallback(AudioDataCallback callback) {
        this.callback = callback;
    }

    public void startRecording() throws Exception {
        if (isRecording) {
            throw new Exception("已在录音中");
        }

        AudioFormat audioFormat = new AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .build();

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        android.util.Log.d("AudioCaptureService", "Buffer size: " + bufferSize);

        boolean usePlaybackCapture = false;

        // 尝试使用AudioPlaybackCapture（系统音频捕获）
        if (mediaProjection != null) {
            try {
                android.util.Log.d("AudioCaptureService", "Attempting AudioPlaybackCapture with MediaProjection");

                AudioPlaybackCaptureConfiguration.Builder configBuilder =
                    new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);

                configBuilder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_MEDIA);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_GAME);

                AudioPlaybackCaptureConfiguration config = configBuilder.build();

                audioRecord = new AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config)
                    .build();

                audioRecord.startRecording();
                usePlaybackCapture = true;
                android.util.Log.i("AudioCaptureService", "AudioPlaybackCapture started successfully");
            } catch (Exception e) {
                android.util.Log.w("AudioCaptureService", "AudioPlaybackCapture failed, falling back to microphone: " + e.getMessage());
                if (audioRecord != null) {
                    try {
                        audioRecord.release();
                    } catch (Exception ignored) {}
                    audioRecord = null;
                }
            }
        }

        // 降级到麦克风录音
        if (!usePlaybackCapture) {
            try {
                android.util.Log.d("AudioCaptureService", "Using microphone recording as fallback");

                audioRecord = new AudioRecord.Builder()
                    .setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .build();

                audioRecord.startRecording();
                android.util.Log.i("AudioCaptureService", "Microphone recording started successfully");
            } catch (Exception e) {
                android.util.Log.e("AudioCaptureService", "Failed to start microphone recording", e);
                throw new Exception("无法启动录音（系统音频捕获和麦克风均失败）: " + e.getMessage());
            }
        }

        isRecording = true;

        // 启动录音线程
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0 && callback != null) {
                    byte[] audioData = new byte[read];
                    System.arraycopy(buffer, 0, audioData, 0, read);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        String base64Audio = Base64.getEncoder().encodeToString(audioData);
                        callback.onAudioData(base64Audio, read);
                    }
                }
            }
        });
        recordingThread.start();
    }

    public void stopRecording() {
        isRecording = false;

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "音频捕获服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("正在捕获会议音频");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meeting Assistant")
            .setContentText("正在录制会议音频...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build();
    }
}
