import AudioCaptureService, { AudioDataEvent } from '../modules/AudioCapture';
import WebSocketService, { TranscriptMessage } from './WebSocketService';
import ApiService from './ApiService';

// const WEBSOCKET_URL = 'ws://10.0.2.2:8080/ws/audio-stream'; // Android模拟器
const WEBSOCKET_URL = 'ws://192.168.207.210:8080/ws/audio-stream'; // 真机使用

export interface Transcript {
  id: number;
  content: string;
  timestamp: string;
  speakerId: number | null;
}

export class MeetingService {
  private meetingId: number | null = null;
  private isRecording = false;

  /**
   * 开始新会议
   */
  async startMeeting(): Promise<number> {
    try {
      // 1. 请求音频捕获权限
      console.log('Requesting audio capture permission...');
      await AudioCaptureService.requestPermission();

      // 2. 连接WebSocket
      console.log('Connecting to WebSocket...');
      this.meetingId = await WebSocketService.connect(WEBSOCKET_URL);

      // 3. 设置音频数据监听
      AudioCaptureService.onAudioData(this.handleAudioData.bind(this));

      // 4. 开始录音
      console.log('Starting audio recording...');
      await AudioCaptureService.startRecording();
      this.isRecording = true;

      return this.meetingId;
    } catch (error) {
      console.error('Failed to start meeting:', error);
      throw error;
    }
  }

  /**
   * 结束会议
   */
  async stopMeeting(): Promise<number | null> {
    try {
      this.isRecording = false;

      // 停止录音（原生层会自动发送剩余音频）
      await AudioCaptureService.stopRecording();

      // 移除音频监听
      AudioCaptureService.removeAudioDataListener();

      // 断开WebSocket
      WebSocketService.disconnect();

      const currentMeetingId = this.meetingId;
      this.meetingId = null;

      // 调用后端完成会议接口，生成总结（同步等待）
      if (currentMeetingId) {
        console.log('Completing meeting and generating summary...');
        await ApiService.completeMeeting(currentMeetingId);
        console.log('Meeting completed successfully');
      }

      console.log('Meeting stopped');
      return currentMeetingId;
    } catch (error) {
      console.error('Failed to stop meeting:', error);
      throw error;
    }
  }

  /**
   * 监听转录结果
   */
  onTranscript(callback: (transcript: Transcript) => void): void {
    WebSocketService.onTranscript((message: TranscriptMessage) => {
      callback({
        id: message.id,
        content: message.content,
        timestamp: message.timestamp,
        speakerId: message.speakerId,
      });
    });
  }

  /**
   * 监听错误
   */
  onError(callback: (error: string) => void): void {
    WebSocketService.onError(callback);
  }

  /**
   * 处理音频数据
   * Android 原生层已经实现了 VAD，只在检测到静音停顿后才发送完整的语句
   */
  private handleAudioData(event: AudioDataEvent): void {
    if (!this.isRecording) return;

    console.log(`Received audio segment after silence detection, length: ${event.length}`);

    // 直接发送（原生层已经做了 VAD 检测和缓冲）
    WebSocketService.sendAudio([event.audioData]);
  }

  /**
   * 获取当前会议ID
   */
  getMeetingId(): number | null {
    return this.meetingId;
  }

  /**
   * 是否正在录音
   */
  getIsRecording(): boolean {
    return this.isRecording;
  }
}

export default new MeetingService();
