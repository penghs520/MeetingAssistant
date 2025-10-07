import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

interface AudioCaptureModule {
  requestPermission(): Promise<string>;
  startRecording(): Promise<string>;
  stopRecording(): Promise<string>;
}

const { AudioCapture } = NativeModules as { AudioCapture: AudioCaptureModule };

const audioCaptureEmitter = new NativeEventEmitter(NativeModules.AudioCapture);

export interface AudioDataEvent {
  audioData: string; // Base64 encoded
  length: number;
}

export class AudioCaptureService {
  private audioDataSubscription: EmitterSubscription | null = null;

  /**
   * 请求音频捕获权限
   */
  async requestPermission(): Promise<void> {
    await AudioCapture.requestPermission();
  }

  /**
   * 开始录音
   */
  async startRecording(): Promise<void> {
    await AudioCapture.startRecording();
  }

  /**
   * 停止录音
   */
  async stopRecording(): Promise<void> {
    await AudioCapture.stopRecording();
  }

  /**
   * 监听音频数据
   */
  onAudioData(callback: (event: AudioDataEvent) => void): void {
    this.audioDataSubscription = audioCaptureEmitter.addListener(
      'onAudioData',
      callback
    );
  }

  /**
   * 移除监听
   */
  removeAudioDataListener(): void {
    if (this.audioDataSubscription) {
      this.audioDataSubscription.remove();
      this.audioDataSubscription = null;
    }
  }
}

export default new AudioCaptureService();
