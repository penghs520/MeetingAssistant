import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import MeetingService, { Transcript } from '../services/MeetingService';

interface MeetingScreenProps {
  navigation: any;
}

export default function MeetingScreen({ navigation }: MeetingScreenProps) {
  const [isRecording, setIsRecording] = useState(false);
  const [meetingId, setMeetingId] = useState<number | null>(null);
  const [transcripts, setTranscripts] = useState<Transcript[]>([]);
  const scrollViewRef = useRef<ScrollView>(null);

  useEffect(() => {
    // 请求麦克风权限
    requestAudioPermission();

    // 监听转录结果
    MeetingService.onTranscript((transcript) => {
      setTranscripts((prev) => [...prev, transcript]);
      // 自动滚动到底部
      setTimeout(() => {
        scrollViewRef.current?.scrollToEnd({ animated: true });
      }, 100);
    });

    // 监听错误
    MeetingService.onError((error) => {
      Alert.alert('错误', error);
    });

    // 拦截物理返回按钮
    const backHandler = navigation.addListener('beforeRemove', (e) => {
      if (!isRecording) {
        // 未在录音，允许正常返回
        return;
      }

      // 阻止默认行为
      e.preventDefault();

      // 提示用户
      Alert.alert(
        '会议正在进行中',
        '确定要结束会议并返回吗？',
        [
          {
            text: '取消',
            style: 'cancel',
          },
          {
            text: '结束会议',
            style: 'destructive',
            onPress: async () => {
              try {
                await MeetingService.stopMeeting();
                setIsRecording(false);
                // 允许返回
                navigation.dispatch(e.data.action);
              } catch (error) {
                Alert.alert('停止失败', String(error));
              }
            },
          },
        ]
      );
    });

    return () => {
      backHandler();
      // 组件卸载时停止会议
      if (isRecording) {
        MeetingService.stopMeeting();
      }
    };
  }, [isRecording, navigation]);

  const requestAudioPermission = async () => {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
          {
            title: '需要录音权限',
            message: '应用需要录音权限来捕获会议音频',
            buttonNeutral: '稍后询问',
            buttonNegative: '拒绝',
            buttonPositive: '允许',
          }
        );
        if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
          Alert.alert('权限被拒绝', '需要录音权限才能使用此功能');
        }
      } catch (err) {
        console.warn(err);
      }
    }
  };

  const handleStartMeeting = async () => {
    try {
      const id = await MeetingService.startMeeting();
      setMeetingId(id);
      setIsRecording(true);
      Alert.alert('会议已开始', `会议ID: ${id}`);
    } catch (error) {
      Alert.alert('启动失败', String(error));
    }
  };

  const handleStopMeeting = async () => {
    const currentMeetingId = meetingId;
    try {
      await MeetingService.stopMeeting();
      setIsRecording(false);

      // 提示用户并导航到详情页
      Alert.alert(
        '会议已结束',
        '正在生成会议总结，请稍候...',
        [
          {
            text: '查看详情',
            onPress: () => {
              if (currentMeetingId) {
                navigation.navigate('MeetingDetail', { meetingId: currentMeetingId });
              }
            },
          },
          {
            text: '稍后查看',
            style: 'cancel',
          },
        ]
      );
    } catch (error) {
      Alert.alert('停止失败', String(error));
    }
  };

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  // 格式化转录文本显示
  const formatTranscripts = (transcripts: Transcript[]) => {
    return transcripts.map((transcript) => {
      const speakerId = transcript.speakerId;
      const speaker = speakerId ? `说话人${speakerId}` : '未知说话人';

      return {
        id: transcript.id,
        speakerId,
        speaker,
        timestamp: transcript.timestamp,
        content: transcript.content,
      };
    });
  };

  return (
    <View style={styles.container}>
      {/* 头部信息 */}
      <View style={styles.header}>
        <Text style={styles.title}>AI会议助手</Text>
        {meetingId && (
          <Text style={styles.meetingId}>会议ID: {meetingId}</Text>
        )}
        <Text style={styles.status}>
          {isRecording ? '🔴 录音中...' : '⚪ 未录音'}
        </Text>
      </View>

      {/* 转录内容显示 */}
      <ScrollView
        ref={scrollViewRef}
        style={styles.transcriptContainer}
        contentContainerStyle={styles.transcriptContent}
      >
        {transcripts.length === 0 ? (
          <Text style={styles.emptyText}>
            {isRecording ? '等待转录结果...' : '点击开始按钮启动会议'}
          </Text>
        ) : (
          formatTranscripts(transcripts).map((item) => (
            <View key={item.id} style={styles.transcriptItem}>
              <Text style={styles.speakerHeader}>
                @{item.speaker}  {formatTime(item.timestamp)}
              </Text>
              <Text style={styles.transcriptText}>{item.content}</Text>
            </View>
          ))
        )}
      </ScrollView>

      {/* 控制按钮 */}
      <View style={styles.controls}>
        <TouchableOpacity
          style={[
            styles.button,
            isRecording ? styles.stopButton : styles.startButton,
          ]}
          onPress={isRecording ? handleStopMeeting : handleStartMeeting}
          activeOpacity={0.8}
        >
          <Text style={styles.buttonText}>
            {isRecording ? '停止会议' : '开始会议'}
          </Text>
        </TouchableOpacity>
      </View>

      {/* 使用说明 */}
      {!isRecording && transcripts.length === 0 && (
        <View style={styles.instructions}>
          <Text style={styles.instructionTitle}>使用说明：</Text>
          <Text style={styles.instructionText}>
            1. 打开其他会议应用（钉钉、腾讯会议等）
          </Text>
          <Text style={styles.instructionText}>
            2. 点击"开始会议"并授予权限
          </Text>
          <Text style={styles.instructionText}>
            3. 系统将自动捕获并转录音频
          </Text>
          <Text style={styles.instructionText}>
            4. 会议结束后点击"停止会议"
          </Text>
        </View>
      )}
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
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 5,
  },
  meetingId: {
    fontSize: 14,
    color: 'white',
    marginBottom: 5,
  },
  status: {
    fontSize: 16,
    color: 'white',
    fontWeight: '600',
  },
  transcriptContainer: {
    flex: 1,
    backgroundColor: 'white',
  },
  transcriptContent: {
    padding: 15,
  },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    marginTop: 50,
    fontSize: 16,
  },
  transcriptItem: {
    marginBottom: 20,
    paddingBottom: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  speakerHeader: {
    fontSize: 14,
    color: '#4CAF50',
    fontWeight: '600',
    marginBottom: 8,
  },
  transcriptText: {
    fontSize: 16,
    color: '#333',
    lineHeight: 24,
  },
  controls: {
    padding: 20,
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  button: {
    padding: 18,
    borderRadius: 12,
    alignItems: 'center',
  },
  startButton: {
    backgroundColor: '#4CAF50',
  },
  stopButton: {
    backgroundColor: '#f44336',
  },
  buttonText: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
  },
  instructions: {
    padding: 20,
    backgroundColor: '#fff3cd',
    borderTopWidth: 1,
    borderTopColor: '#ffc107',
  },
  instructionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#856404',
    marginBottom: 10,
  },
  instructionText: {
    fontSize: 14,
    color: '#856404',
    marginBottom: 5,
  },
});
