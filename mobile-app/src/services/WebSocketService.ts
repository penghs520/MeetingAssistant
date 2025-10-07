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
        console.error('Error type:', typeof error);
        console.error('Error details:', JSON.stringify(error));
        reject(error);
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket closed');
        console.log('Close code:', event.code);
        console.log('Close reason:', event.reason);
        console.log('Was clean:', event.wasClean);
        this.attemptReconnect(serverUrl, meetingId);
      };
    });
  }

  /**
   * 发送音频数据（Base64编码的音频数据数组）
   */
  sendAudio(audioDataArray: string[]): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        // 计算总长度
        let totalLength = 0;
        const decodedArrays: Uint8Array[] = [];

        // 将所有Base64字符串解码为字节数组
        for (const audioData of audioDataArray) {
          try {
            const binaryString = atob(audioData);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
              bytes[i] = binaryString.charCodeAt(i);
            }
            decodedArrays.push(bytes);
            totalLength += bytes.length;
          } catch (error) {
            console.error('Failed to decode audio data:', error);
            console.error('Invalid base64 string length:', audioData.length);
            console.error('First 50 chars:', audioData.substring(0, 50));
            // 跳过这个损坏的数据块，继续处理其他的
            continue;
          }
        }

        if (decodedArrays.length === 0) {
          console.warn('No valid audio data to send');
          return;
        }

        // 合并所有字节数组
        const combined = new Uint8Array(totalLength);
        let offset = 0;
        for (const arr of decodedArrays) {
          combined.set(arr, offset);
          offset += arr.length;
        }

        // 发送合并后的数据
        console.log(`Sending ${totalLength} bytes of audio data (${decodedArrays.length} chunks)`);
        this.ws.send(combined.buffer);
      } catch (error) {
        console.error('Error sending audio data:', error);
      }
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
