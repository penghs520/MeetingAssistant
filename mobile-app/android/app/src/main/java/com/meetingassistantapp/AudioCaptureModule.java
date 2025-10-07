package com.meetingassistantapp;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Base64;

/**
 * Android系统音频捕获模块
 * 使用AudioPlaybackCapture API捕获其他应用的音频输出
 */
public class AudioCaptureModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final String MODULE_NAME = "AudioCapture";
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 1001;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private Promise permissionPromise;

    public AudioCaptureModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionManager = (MediaProjectionManager) reactContext.getSystemService(
                ReactApplicationContext.MEDIA_PROJECTION_SERVICE
            );
        }
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    /**
     * 请求屏幕录制权限（用于捕获系统音频）
     */
    @ReactMethod
    public void requestPermission(Promise promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            promise.reject("API_LEVEL_ERROR", "需要Android 10 (API 29)及以上版本");
            return;
        }

        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "无法获取Activity");
            return;
        }

        permissionPromise = promise;
        Intent intent = projectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE);
    }

    /**
     * 开始录音
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @ReactMethod
    public void startRecording(Promise promise) {
        if (mediaProjection == null) {
            promise.reject("NO_PERMISSION", "请先申请权限");
            return;
        }

        if (isRecording) {
            promise.reject("ALREADY_RECORDING", "已在录音中");
            return;
        }

        try {
            // 配置音频捕获
            AudioPlaybackCaptureConfiguration config =
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build();

            AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build();

            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

            audioRecord = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build();

            audioRecord.startRecording();
            isRecording = true;

            // 启动录音线程
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        sendAudioData(buffer, read);
                    }
                }
            });
            recordingThread.start();

            promise.resolve("录音已开始");
        } catch (Exception e) {
            promise.reject("START_ERROR", e.getMessage());
        }
    }

    /**
     * 停止录音
     */
    @ReactMethod
    public void stopRecording(Promise promise) {
        if (!isRecording) {
            promise.reject("NOT_RECORDING", "当前未在录音");
            return;
        }

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

        promise.resolve("录音已停止");
    }

    /**
     * 发送音频数据到React Native
     */
    private void sendAudioData(byte[] data, int length) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 只发送有效数据
            byte[] audioData = new byte[length];
            System.arraycopy(data, 0, audioData, 0, length);

            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            WritableMap params = Arguments.createMap();
            params.putString("audioData", base64Audio);
            params.putInt("length", length);

            getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onAudioData", params);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && permissionPromise != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    permissionPromise.resolve("权限已授予");
                }
            } else if (permissionPromise != null) {
                permissionPromise.reject("PERMISSION_DENIED", "用户拒绝授权");
            }
            permissionPromise = null;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // 不需要处理
    }
}
