export interface TranscriptMessage {
  type: 'transcript';
  id: number;
  content: string;
  timestamp: string;
  speakerId: number | null;
}

export interface ConnectedMessage {
  type: 'connected';
  meetingId: number;
  message: string;
}

export interface ErrorMessage {
  type: 'error';
  message: string;
}

type WebSocketMessage = TranscriptMessage | ConnectedMessage | ErrorMessage;

export class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private onTranscriptCallback: ((transcript: TranscriptMessage) => void) | null = null;
  private onConnectedCallback: ((meetingId: number) => void) | null = null;
  private onErrorCallback: ((error: string) => void) | null = null;

  /**
   * 连接到WebSocket服务器
   */
  connect(serverUrl: string, meetingId?: number): Promise<number> {
    return new Promise((resolve, reject) => {
      const url = meetingId
        ? `${serverUrl}?meetingId=${meetingId}`
        : serverUrl;

      console.log('Connecting to WebSocket:', url);

      this.ws = new WebSocket(url);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
      };

      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);

          switch (message.type) {
            case 'connected':
              console.log('Meeting connected:', message.meetingId);
              if (this.onConnectedCallback) {
                this.onConnectedCallback(message.meetingId);
              }
              resolve(message.meetingId);
              break;

            case 'transcript':
              console.log('Transcript received:', message.content);
              if (this.onTranscriptCallback) {
                this.onTranscriptCallback(message);
              }
              break;

            case 'error':
              console.error('Server error:', message.message);
              if (this.onErrorCallback) {
                this.onErrorCallback(message.message);
              }
              break;
          }
        } catch (error) {
          console.error('Failed to parse message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      };

      this.ws.onclose = () => {
        console.log('WebSocket closed');
        this.attemptReconnect(serverUrl, meetingId);
      };
    });
  }

  /**
   * 发送音频数据
   */
  sendAudio(audioData: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      // 将Base64字符串转换为ArrayBuffer
      const binaryString = atob(audioData);
      const bytes = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }
      this.ws.send(bytes.buffer);
    } else {
      console.warn('WebSocket not connected, cannot send audio');
    }
  }

  /**
   * 监听转录结果
   */
  onTranscript(callback: (transcript: TranscriptMessage) => void): void {
    this.onTranscriptCallback = callback;
  }

  /**
   * 监听连接成功
   */
  onConnected(callback: (meetingId: number) => void): void {
    this.onConnectedCallback = callback;
  }

  /**
   * 监听错误
   */
  onError(callback: (error: string) => void): void {
    this.onErrorCallback = callback;
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.reconnectAttempts = this.maxReconnectAttempts; // 防止自动重连
  }

  /**
   * 尝试重连
   */
  private attemptReconnect(serverUrl: string, meetingId?: number): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(
        `Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
      );

      setTimeout(() => {
        this.connect(serverUrl, meetingId).catch((error) => {
          console.error('Reconnection failed:', error);
        });
      }, this.reconnectDelay);
    } else {
      console.error('Max reconnection attempts reached');
      if (this.onErrorCallback) {
        this.onErrorCallback('连接已断开，请重新启动会议');
      }
    }
  }
}

export default new WebSocketService();
