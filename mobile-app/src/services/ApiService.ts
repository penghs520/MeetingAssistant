import axios from 'axios';

// const API_BASE_URL = 'http://10.0.2.2:8080/api'; // Android模拟器
const API_BASE_URL = 'http://192.168.207.210:8080/api'; // 真机使用

export interface Meeting {
  id: number;
  title: string;
  startTime: string;
  endTime?: string;
  status: 'RECORDING' | 'COMPLETED';
  summary?: string;
}

export interface Transcript {
  id: number;
  content: string;
  timestamp: string;
  speakerId: number | null;
  sequenceOrder: number;
}

export interface Speaker {
  id: number;
  name: string;
  color: string;
}

export class ApiService {
  /**
   * 创建新会议
   */
  async createMeeting(title: string): Promise<Meeting> {
    const response = await axios.post(`${API_BASE_URL}/meetings`, { title });
    return response.data;
  }

  /**
   * 完成会议（生成总结）
   */
  async completeMeeting(meetingId: number): Promise<Meeting> {
    const response = await axios.post(`${API_BASE_URL}/meetings/${meetingId}/complete`);
    return response.data;
  }

  /**
   * 获取会议详情
   */
  async getMeeting(meetingId: number): Promise<Meeting> {
    const response = await axios.get(`${API_BASE_URL}/meetings/${meetingId}`);
    return response.data;
  }

  /**
   * 获取所有会议列表
   */
  async getAllMeetings(): Promise<Meeting[]> {
    const response = await axios.get(`${API_BASE_URL}/meetings`);
    return response.data;
  }

  /**
   * 删除会议
   */
  async deleteMeeting(meetingId: number): Promise<void> {
    await axios.delete(`${API_BASE_URL}/meetings/${meetingId}`);
  }

  /**
   * 获取会议的所有转录
   */
  async getTranscripts(meetingId: number): Promise<Transcript[]> {
    const response = await axios.get(`${API_BASE_URL}/meetings/${meetingId}/transcripts`);
    return response.data;
  }

  /**
   * 获取会议的所有说话人
   */
  async getSpeakers(meetingId: number): Promise<Speaker[]> {
    const response = await axios.get(`${API_BASE_URL}/meetings/${meetingId}/speakers`);
    return response.data;
  }
}

export default new ApiService();
