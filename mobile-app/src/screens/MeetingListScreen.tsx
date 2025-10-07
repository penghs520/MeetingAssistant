import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  Alert,
} from 'react-native';

const API_BASE_URL = 'http://192.168.207.210:8080';

interface Meeting {
  id: number;
  title: string;
  startTime: string;
  endTime: string | null;
  summary: string | null;
  status: string;
}

interface MeetingListScreenProps {
  navigation: any;
}

export default function MeetingListScreen({ navigation }: MeetingListScreenProps) {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [hasOngoingMeeting, setHasOngoingMeeting] = useState(false);

  useEffect(() => {
    loadMeetings();

    // 监听页面焦点，每次进入页面时刷新会议列表
    const unsubscribe = navigation.addListener('focus', () => {
      loadMeetings();
    });

    return unsubscribe;
  }, [navigation]);

  const loadMeetings = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/meetings`);
      if (!response.ok) {
        throw new Error('Failed to load meetings');
      }
      const data = await response.json();
      setMeetings(data);

      // 检查是否有进行中的会议（RECORDING 状态）
      const ongoing = data.some((meeting: Meeting) => meeting.status === 'RECORDING');
      setHasOngoingMeeting(ongoing);
    } catch (error) {
      Alert.alert('加载失败', String(error));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleNewMeeting = () => {
    if (hasOngoingMeeting) {
      Alert.alert(
        '提示',
        '已有会议正在进行中，将进入该会议',
        [
          {
            text: '取消',
            style: 'cancel',
          },
          {
            text: '进入会议',
            onPress: () => navigation.navigate('Meeting'),
          },
        ]
      );
    } else {
      navigation.navigate('Meeting');
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    loadMeetings();
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'RECORDING':
        return '录音中';
      case 'COMPLETED':
        return '已完成';
      default:
        return status;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RECORDING':
        return '#4CAF50';
      case 'COMPLETED':
        return '#2196F3';
      default:
        return '#999';
    }
  };

  const renderMeetingItem = ({ item }: { item: Meeting }) => (
    <TouchableOpacity
      style={styles.meetingItem}
      onPress={() => navigation.navigate('MeetingDetail', { meetingId: item.id })}
    >
      <View style={styles.meetingHeader}>
        <Text style={styles.meetingTitle}>{item.title}</Text>
        <View
          style={[
            styles.statusBadge,
            { backgroundColor: getStatusColor(item.status) },
          ]}
        >
          <Text style={styles.statusText}>{getStatusText(item.status)}</Text>
        </View>
      </View>

      <Text style={styles.meetingTime}>
        开始时间: {formatDateTime(item.startTime)}
      </Text>

      {item.endTime && (
        <Text style={styles.meetingTime}>
          结束时间: {formatDateTime(item.endTime)}
        </Text>
      )}

      {item.summary && (
        <Text style={styles.meetingSummary} numberOfLines={3}>
          {item.summary}
        </Text>
      )}
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>会议列表</Text>
        <TouchableOpacity
          style={[
            styles.newMeetingButton,
            hasOngoingMeeting && styles.ongoingMeetingButton,
          ]}
          onPress={handleNewMeeting}
        >
          <Text style={styles.newMeetingButtonText}>
            {hasOngoingMeeting ? '进入会议' : '+ 新会议'}
          </Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={meetings}
        renderItem={renderMeetingItem}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>
              {loading ? '加载中...' : '暂无会议记录'}
            </Text>
            {!loading && (
              <TouchableOpacity
                style={styles.startFirstMeetingButton}
                onPress={handleNewMeeting}
              >
                <Text style={styles.startFirstMeetingText}>开始第一个会议</Text>
              </TouchableOpacity>
            )}
          </View>
        }
      />
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
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
  },
  newMeetingButton: {
    backgroundColor: 'white',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
  },
  ongoingMeetingButton: {
    backgroundColor: '#FFA726',
  },
  newMeetingButtonText: {
    color: '#4CAF50',
    fontWeight: 'bold',
    fontSize: 16,
  },
  listContent: {
    padding: 15,
  },
  meetingItem: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  meetingHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  meetingTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
  },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    color: 'white',
    fontSize: 12,
    fontWeight: 'bold',
  },
  meetingTime: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  meetingSummary: {
    fontSize: 14,
    color: '#888',
    marginTop: 8,
    lineHeight: 20,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 100,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginBottom: 20,
  },
  startFirstMeetingButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 30,
    paddingVertical: 15,
    borderRadius: 25,
  },
  startFirstMeetingText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
