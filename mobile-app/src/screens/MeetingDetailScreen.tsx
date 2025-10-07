import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  TouchableOpacity,
} from 'react-native';

const API_BASE_URL = 'http://192.168.207.210:8080';

interface Speaker {
  id: number;
  name: string;
  color: string;
}

interface Transcript {
  id: number;
  content: string;
  timestamp: string;
  speakerId: number | null;
  speaker: Speaker | null;
}

interface Meeting {
  id: number;
  title: string;
  startTime: string;
  endTime: string | null;
  summary: string | null;
  status: string;
}

interface MeetingDetailScreenProps {
  route: any;
  navigation: any;
}

export default function MeetingDetailScreen({
  route,
  navigation,
}: MeetingDetailScreenProps) {
  const { meetingId } = route.params;
  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [transcripts, setTranscripts] = useState<Transcript[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadMeetingDetail();
  }, [meetingId]);

  const loadMeetingDetail = async () => {
    try {
      // 加载会议信息
      const meetingResponse = await fetch(
        `${API_BASE_URL}/api/meetings/${meetingId}`
      );
      if (!meetingResponse.ok) {
        throw new Error('Failed to load meeting');
      }
      const meetingData = await meetingResponse.json();
      setMeeting(meetingData);

      // 加载转录记录
      const transcriptsResponse = await fetch(
        `${API_BASE_URL}/api/meetings/${meetingId}/transcripts`
      );
      if (!transcriptsResponse.ok) {
        throw new Error('Failed to load transcripts');
      }
      const transcriptsData = await transcriptsResponse.json();
      setTranscripts(transcriptsData);
    } catch (error) {
      Alert.alert('加载失败', String(error));
    } finally {
      setLoading(false);
    }
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4CAF50" />
        <Text style={styles.loadingText}>加载中...</Text>
      </View>
    );
  }

  if (!meeting) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>会议不存在</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>会议详情</Text>
      </View>

      <ScrollView style={styles.content}>
        {/* 会议信息 */}
        <View style={styles.infoCard}>
          <Text style={styles.title}>{meeting.title}</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>会议ID:</Text>
            <Text style={styles.infoValue}>{meeting.id}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>开始时间:</Text>
            <Text style={styles.infoValue}>
              {formatDateTime(meeting.startTime)}
            </Text>
          </View>
          {meeting.endTime && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>结束时间:</Text>
              <Text style={styles.infoValue}>
                {formatDateTime(meeting.endTime)}
              </Text>
            </View>
          )}
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>状态:</Text>
            <Text style={styles.infoValue}>
              {meeting.status === 'COMPLETED' ? '已完成' : '进行中'}
            </Text>
          </View>
        </View>

        {/* 会议总结 */}
        {meeting.summary && (
          <View style={styles.summaryCard}>
            <Text style={styles.sectionTitle}>会议总结</Text>
            <Text style={styles.summaryText}>{meeting.summary}</Text>
          </View>
        )}

        {/* 文字记录 */}
        <View style={styles.transcriptsCard}>
          <Text style={styles.sectionTitle}>
            文字记录 ({transcripts.length})
          </Text>
          {transcripts.length === 0 ? (
            <Text style={styles.emptyText}>暂无文字记录</Text>
          ) : (
            <View style={styles.transcriptList}>
              {transcripts.map((transcript, index) => (
                <View key={transcript.id}>
                  <View style={styles.transcriptLine}>
                    <Text style={styles.speakerName}>
                      @{transcript.speaker?.name || '未知'}
                    </Text>
                    <Text style={styles.transcriptTime}>
                      {formatTime(transcript.timestamp)}
                    </Text>
                  </View>
                  <Text style={styles.transcriptText}>
                    {transcript.content}
                  </Text>
                  {index < transcripts.length - 1 && (
                    <View style={styles.transcriptDivider} />
                  )}
                </View>
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#4CAF50',
    padding: 20,
    paddingTop: 50,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: 'white',
  },
  content: {
    flex: 1,
    padding: 15,
  },
  infoCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 12,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
  },
  infoRow: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  infoLabel: {
    fontSize: 14,
    color: '#666',
    width: 80,
  },
  infoValue: {
    fontSize: 14,
    color: '#333',
    flex: 1,
  },
  summaryCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 12,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  transcriptsCard: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 12,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  summaryText: {
    fontSize: 15,
    color: '#555',
    lineHeight: 24,
  },
  transcriptList: {
    paddingVertical: 5,
    paddingHorizontal: 5,
  },
  transcriptLine: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
    marginTop: 12,
  },
  speakerName: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#4CAF50',
    marginRight: 12,
  },
  transcriptTime: {
    fontSize: 13,
    color: '#999',
  },
  transcriptText: {
    fontSize: 15,
    color: '#333',
    lineHeight: 24,
    marginBottom: 12,
  },
  transcriptDivider: {
    height: 1,
    backgroundColor: '#f0f0f0',
    marginVertical: 8,
  },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    fontSize: 14,
    paddingVertical: 20,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  errorText: {
    fontSize: 16,
    color: '#999',
  },
});
