package com.meeting.assistant.service;

import com.meeting.assistant.entity.Meeting;
import com.meeting.assistant.entity.Transcript;
import com.meeting.assistant.repository.MeetingRepository;
import com.meeting.assistant.repository.TranscriptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TranscriptionService {

    private final TranscriptRepository transcriptRepository;
    private final MeetingRepository meetingRepository;

    public TranscriptionService(TranscriptRepository transcriptRepository,
                              MeetingRepository meetingRepository) {
        this.transcriptRepository = transcriptRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional
    public Transcript saveTranscript(Long meetingId, String content, LocalDateTime timestamp) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        // 智能合并：检查是否应该与最近一条转录合并
        List<Transcript> recentTranscripts = transcriptRepository
            .findByMeetingIdOrderBySequenceOrderDesc(meetingId);

        if (!recentTranscripts.isEmpty()) {
            Transcript lastTranscript = recentTranscripts.get(0);

            // 计算时间间隔（秒）
            long secondsBetween = java.time.Duration.between(
                lastTranscript.getTimestamp(),
                timestamp
            ).getSeconds();

            // 如果间隔小于2秒，则合并（说话人识别在后续步骤处理）
            // 目前简化处理：只要时间间隔短就合并
            if (secondsBetween < 2) {
                // 合并到上一条记录
                String mergedContent = lastTranscript.getContent() + " " + content;
                lastTranscript.setContent(mergedContent);
                Transcript updated = transcriptRepository.save(lastTranscript);
                log.info("Merged transcript into {} for meeting {} (interval: {}s)",
                    updated.getId(), meetingId, secondsBetween);
                return updated;
            }
        }

        // 创建新的转录记录
        Transcript transcript = new Transcript();
        transcript.setMeeting(meeting);
        transcript.setContent(content);
        transcript.setTimestamp(timestamp);

        // 设置序列号
        Integer currentCount = transcriptRepository.countByMeetingId(meetingId);
        transcript.setSequenceOrder(currentCount != null ? currentCount + 1 : 1);

        Transcript saved = transcriptRepository.save(transcript);
        log.info("Saved transcript {} for meeting {}", saved.getId(), meetingId);
        return saved;
    }

    @Transactional
    public Transcript updateSpeaker(Long transcriptId, Long speakerId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
            .orElseThrow(() -> new RuntimeException("Transcript not found: " + transcriptId));

        // Speaker会在SpeakerService中处理
        log.info("Updated speaker for transcript {}", transcriptId);
        return transcriptRepository.save(transcript);
    }

    public List<Transcript> getTranscriptsByMeeting(Long meetingId) {
        return transcriptRepository.findByMeetingIdOrderBySequenceOrderAsc(meetingId);
    }
}
