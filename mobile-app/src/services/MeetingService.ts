import AudioCaptureService, { AudioDataEvent } from '../modules/AudioCapture';
import WebSocketService, { TranscriptMessage } from './WebSocketService';

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
  private audioBuffer: string[] = [];
  private bufferSize = 3; // 缓冲3段音频再发送（约3秒）

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
   * 停止会议
   */
  async stopMeeting(): Promise<void> {
    try {
      this.isRecording = false;

      // 停止录音
      await AudioCaptureService.stopRecording();

      // 移除音频监听
      AudioCaptureService.removeAudioDataListener();

      // 发送剩余的音频数据
      if (this.audioBuffer.length > 0) {
        this.flushAudioBuffer();
      }

      // 断开WebSocket
      WebSocketService.disconnect();

      this.meetingId = null;
      console.log('Meeting stopped');
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
   */
  private handleAudioData(event: AudioDataEvent): void {
    if (!this.isRecording) return;

    // 添加到缓冲区
    this.audioBuffer.push(event.audioData);

    // 当缓冲区满时发送
    if (this.audioBuffer.length >= this.bufferSize) {
      this.flushAudioBuffer();
    }
  }

  /**
   * 发送缓冲区的音频数据
   */
  private flushAudioBuffer(): void {
    if (this.audioBuffer.length === 0) return;

    // 合并所有音频数据
    const combinedAudio = this.audioBuffer.join('');

    // 通过WebSocket发送
    WebSocketService.sendAudio(combinedAudio);

    // 清空缓冲区
    this.audioBuffer = [];
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
