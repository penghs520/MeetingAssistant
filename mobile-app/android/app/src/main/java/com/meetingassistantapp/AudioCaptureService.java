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

    // VAD (Voice Activity Detection) 参数
    private static final int SILENCE_THRESHOLD = 800;      // 静音振幅阈值
    private static final int SILENCE_DURATION_MS = 800;    // 静音持续时间（毫秒）
    private static final int MIN_AUDIO_LENGTH_MS = 300;    // 最小音频长度（毫秒），过滤短暂噪音（降低以避免误过滤短句）
    private static final int MIN_ENERGY_THRESHOLD = 500;   // 最小能量阈值，过滤低能量噪音（降低以避免误过滤正常说话）

    private long lastSoundTime = System.currentTimeMillis();
    private long speechStartTime = 0;                      // 开始说话的时间
    private boolean isSpeaking = false;                    // 是否正在说话
    private java.util.List<byte[]> audioBuffer = new java.util.ArrayList<>();

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

        // 启动录音线程（带 VAD 检测）
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0 && callback != null) {
                    byte[] audioData = new byte[read];
                    System.arraycopy(buffer, 0, audioData, 0, read);

                    // 添加到缓冲区
                    audioBuffer.add(audioData);

                    // 检测是否为静音
                    boolean isSilence = detectSilence(audioData);

                    if (isSilence) {
                        // 检测到静音停顿，发送缓冲区的所有音频
                        flushAudioBuffer();
                    }
                }
            }
        });
        recordingThread.start();
    }

    /**
     * 检测音频数据是否为静音
     * @param audioData 16-bit PCM 音频数据
     * @return true 如果持续静音超过阈值时间
     */
    private boolean detectSilence(byte[] audioData) {
        // 计算音频振幅（16-bit PCM）
        long sum = 0;
        for (int i = 0; i < audioData.length - 1; i += 2) {
            // 将两个字节合并为一个 16-bit 采样值
            int sample = (audioData[i] & 0xFF) | (audioData[i + 1] << 8);
            sum += Math.abs(sample);
        }
        long avgAmplitude = sum / (audioData.length / 2);

        // 判断是否有声音（超过静音阈值）
        if (avgAmplitude > SILENCE_THRESHOLD) {
            // 记录说话开始时间
            if (!isSpeaking) {
                isSpeaking = true;
                speechStartTime = System.currentTimeMillis();
                android.util.Log.d("AudioCaptureService", "Speech started, amplitude: " + avgAmplitude);
            }
            lastSoundTime = System.currentTimeMillis();
            return false;
        }

        // 当前是静音
        if (isSpeaking) {
            // 检查静音持续时间
            long silenceDuration = System.currentTimeMillis() - lastSoundTime;
            if (silenceDuration >= SILENCE_DURATION_MS) {
                // 说话结束，检查是否为有效语音（非噪音）
                long speechDuration = System.currentTimeMillis() - speechStartTime;
                android.util.Log.d("AudioCaptureService", "Speech ended, duration: " + speechDuration + "ms");

                isSpeaking = false;
                return true;  // 静音结束，触发发送
            }
        }

        return false;
    }

    /**
     * 发送缓冲区中的所有音频数据（带噪音过滤）
     */
    private void flushAudioBuffer() {
        if (audioBuffer.isEmpty()) {
            return;
        }

        // 计算说话总时长
        long speechDuration = System.currentTimeMillis() - speechStartTime;

        // 只过滤非常短的音频（如单次咳嗽、敲击声），避免误过滤正常说话
        if (speechDuration < MIN_AUDIO_LENGTH_MS) {
            android.util.Log.d("AudioCaptureService",
                "Discarded short audio (noise): " + speechDuration + "ms < " + MIN_AUDIO_LENGTH_MS + "ms");
            audioBuffer.clear();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 计算总长度
            int totalLength = 0;
            for (byte[] data : audioBuffer) {
                totalLength += data.length;
            }

            // 合并所有音频数据
            byte[] combined = new byte[totalLength];
            int offset = 0;
            for (byte[] data : audioBuffer) {
                System.arraycopy(data, 0, combined, offset, data.length);
                offset += data.length;
            }

            // 计算平均能量（用于日志，不用于过滤）
            long totalEnergy = 0;
            for (int i = 0; i < combined.length - 1; i += 2) {
                int sample = (combined[i] & 0xFF) | (combined[i + 1] << 8);
                totalEnergy += Math.abs(sample);
            }
            long avgEnergy = totalEnergy / (combined.length / 2);

            // Base64 编码并发送
            String base64Audio = Base64.getEncoder().encodeToString(combined);
            if (callback != null) {
                callback.onAudioData(base64Audio, totalLength);
            }

            android.util.Log.d("AudioCaptureService",
                "Sent audio: " + totalLength + " bytes, duration: " + speechDuration + "ms, energy: " + avgEnergy);
        }

        // 清空缓冲区
        audioBuffer.clear();
    }

    public void stopRecording() {
        isRecording = false;

        // 停止前发送剩余的音频数据
        flushAudioBuffer();

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
