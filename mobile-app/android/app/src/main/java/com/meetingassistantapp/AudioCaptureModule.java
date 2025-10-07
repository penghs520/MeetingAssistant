package com.meetingassistantapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.app.Activity;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Android系统音频捕获模块
 * 使用AudioPlaybackCapture API捕获其他应用的音频输出
 */
public class AudioCaptureModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final String MODULE_NAME = "AudioCapture";
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 1001;

    private MediaProjectionManager projectionManager;
    private Intent mediaProjectionIntent;
    private int mediaProjectionResultCode;
    private AudioCaptureService audioCaptureService;
    private boolean isServiceBound = false;
    private Promise permissionPromise;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioCaptureService.LocalBinder binder = (AudioCaptureService.LocalBinder) service;
            audioCaptureService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioCaptureService = null;
            isServiceBound = false;
        }
    };

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
        if (mediaProjectionIntent == null) {
            promise.reject("NO_PERMISSION", "请先申请权限");
            return;
        }

        try {
            // 启动前台服务
            Intent serviceIntent = new Intent(getReactApplicationContext(), AudioCaptureService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getReactApplicationContext().startForegroundService(serviceIntent);
            } else {
                getReactApplicationContext().startService(serviceIntent);
            }

            // 绑定服务
            getReactApplicationContext().bindService(
                serviceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            );

            // 等待服务连接后启动录音
            new Thread(() -> {
                int attempts = 0;
                while (!isServiceBound && attempts < 50) {
                    try {
                        Thread.sleep(100);
                        attempts++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (isServiceBound && audioCaptureService != null) {
                    try {
                        // 在前台服务中创建MediaProjection（Android 14+要求）
                        audioCaptureService.createMediaProjection(
                            mediaProjectionResultCode,
                            mediaProjectionIntent
                        );

                        // 等待MediaProjection完全初始化
                        Thread.sleep(500);

                        audioCaptureService.setAudioDataCallback((base64Audio, length) -> {
                            WritableMap params = Arguments.createMap();
                            params.putString("audioData", base64Audio);
                            params.putInt("length", length);

                            getReactApplicationContext()
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("onAudioData", params);
                        });
                        audioCaptureService.startRecording();
                        promise.resolve("录音已开始");
                    } catch (Exception e) {
                        promise.reject("START_ERROR", e.getMessage());
                    }
                } else {
                    promise.reject("SERVICE_ERROR", "无法连接到音频捕获服务");
                }
            }).start();

        } catch (Exception e) {
            promise.reject("START_ERROR", e.getMessage());
        }
    }

    /**
     * 停止录音
     */
    @ReactMethod
    public void stopRecording(Promise promise) {
        try {
            if (audioCaptureService != null) {
                audioCaptureService.stopRecording();
            }

            if (isServiceBound) {
                getReactApplicationContext().unbindService(serviceConnection);
                isServiceBound = false;
            }

            Intent serviceIntent = new Intent(getReactApplicationContext(), AudioCaptureService.class);
            getReactApplicationContext().stopService(serviceIntent);

            promise.resolve("录音已停止");
        } catch (Exception e) {
            promise.reject("STOP_ERROR", e.getMessage());
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && permissionPromise != null) {
                // 只保存Intent数据，不在Activity中创建MediaProjection
                // MediaProjection必须在前台服务中创建（Android 14+要求）
                mediaProjectionResultCode = resultCode;
                mediaProjectionIntent = data;
                permissionPromise.resolve("权限已授予");
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
