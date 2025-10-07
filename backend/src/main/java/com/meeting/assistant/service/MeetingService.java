package com.meeting.assistant.service;

import com.meeting.assistant.ai.AIService;
import com.meeting.assistant.entity.Meeting;
import com.meeting.assistant.entity.Speaker;
import com.meeting.assistant.entity.Transcript;
import com.meeting.assistant.repository.MeetingRepository;
import com.meeting.assistant.repository.SpeakerRepository;
import com.meeting.assistant.repository.TranscriptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final SpeakerRepository speakerRepository;
    private final TranscriptRepository transcriptRepository;
    private final AIService aiService;

    public MeetingService(MeetingRepository meetingRepository,
                         SpeakerRepository speakerRepository,
                         TranscriptRepository transcriptRepository,
                         AIService aiService) {
        this.meetingRepository = meetingRepository;
        this.speakerRepository = speakerRepository;
        this.transcriptRepository = transcriptRepository;
        this.aiService = aiService;
    }

    @Transactional
    public Meeting createMeeting(String title) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);
        meeting.setStartTime(LocalDateTime.now());
        meeting.setStatus(Meeting.MeetingStatus.RECORDING);

        Meeting saved = meetingRepository.save(meeting);
        log.info("Created new meeting: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Meeting completeMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        meeting.setEndTime(LocalDateTime.now());
        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);

        // 生成AI总结
        List<Transcript> transcripts = transcriptRepository.findByMeetingIdOrderBySequenceOrderAsc(meetingId);
        List<Speaker> speakers = speakerRepository.findByMeetingId(meetingId);

        if (!transcripts.isEmpty()) {
            String fullTranscript = transcripts.stream()
                .map(t -> {
                    String speakerName = t.getSpeaker() != null ? t.getSpeaker().getName() : "未知";
                    return String.format("[%s] %s", speakerName, t.getContent());
                })
                .collect(Collectors.joining("\n"));

            String summary = aiService.summarize(fullTranscript, speakers);
            meeting.setSummary(summary);
        }

        Meeting saved = meetingRepository.save(meeting);
        log.info("Completed meeting: {}", meetingId);
        return saved;
    }

    public Meeting getMeeting(Long id) {
        return meetingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Meeting not found: " + id));
    }

    public List<Meeting> getAllMeetings() {
        return meetingRepository.findAllByOrderByStartTimeDesc();
    }

    @Transactional
    public void deleteMeeting(Long id) {
        meetingRepository.deleteById(id);
        log.info("Deleted meeting: {}", id);
    }
}
