package com.meeting.assistant.service;

import com.meeting.assistant.entity.Meeting;
import com.meeting.assistant.entity.Speaker;
import com.meeting.assistant.repository.MeetingRepository;
import com.meeting.assistant.repository.SpeakerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SpeakerService {

    private final SpeakerRepository speakerRepository;
    private final MeetingRepository meetingRepository;

    public SpeakerService(SpeakerRepository speakerRepository,
                         MeetingRepository meetingRepository) {
        this.speakerRepository = speakerRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional
    public Speaker createSpeaker(Long meetingId, String name, String color) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        Speaker speaker = new Speaker();
        speaker.setMeeting(meeting);
        speaker.setName(name);
        speaker.setColor(color);

        Speaker saved = speakerRepository.save(speaker);
        log.info("Created speaker {} for meeting {}", saved.getId(), meetingId);
        return saved;
    }

    public List<Speaker> getSpeakersByMeeting(Long meetingId) {
        return speakerRepository.findByMeetingId(meetingId);
    }

    @Transactional
    public Speaker updateSpeaker(Long speakerId, String name, String color) {
        Speaker speaker = speakerRepository.findById(speakerId)
            .orElseThrow(() -> new RuntimeException("Speaker not found: " + speakerId));

        if (name != null) {
            speaker.setName(name);
        }
        if (color != null) {
            speaker.setColor(color);
        }

        return speakerRepository.save(speaker);
    }

    @Transactional
    public void deleteSpeaker(Long speakerId) {
        speakerRepository.deleteById(speakerId);
        log.info("Deleted speaker: {}", speakerId);
    }
}
