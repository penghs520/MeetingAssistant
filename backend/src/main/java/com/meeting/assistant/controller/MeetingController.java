package com.meeting.assistant.controller;

import com.meeting.assistant.entity.Meeting;
import com.meeting.assistant.entity.Speaker;
import com.meeting.assistant.entity.Transcript;
import com.meeting.assistant.service.MeetingService;
import com.meeting.assistant.service.SpeakerService;
import com.meeting.assistant.service.TranscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingService meetingService;
    private final SpeakerService speakerService;
    private final TranscriptionService transcriptionService;

    public MeetingController(MeetingService meetingService,
                           SpeakerService speakerService,
                           TranscriptionService transcriptionService) {
        this.meetingService = meetingService;
        this.speakerService = speakerService;
        this.transcriptionService = transcriptionService;
    }

    @PostMapping
    public ResponseEntity<Meeting> createMeeting(@RequestBody Map<String, String> request) {
        String title = request.getOrDefault("title", "新会议");
        Meeting meeting = meetingService.createMeeting(title);
        return ResponseEntity.ok(meeting);
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> getAllMeetings() {
        List<Meeting> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.getMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Meeting> completeMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.completeMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/transcripts")
    public ResponseEntity<List<Transcript>> getTranscripts(@PathVariable Long id) {
        List<Transcript> transcripts = transcriptionService.getTranscriptsByMeeting(id);
        return ResponseEntity.ok(transcripts);
    }

    @PostMapping("/{id}/speakers")
    public ResponseEntity<Speaker> createSpeaker(
        @PathVariable Long id,
        @RequestBody Map<String, String> request
    ) {
        String name = request.get("name");
        String color = request.getOrDefault("color", "#000000");
        Speaker speaker = speakerService.createSpeaker(id, name, color);
        return ResponseEntity.ok(speaker);
    }

    @GetMapping("/{id}/speakers")
    public ResponseEntity<List<Speaker>> getSpeakers(@PathVariable Long id) {
        List<Speaker> speakers = speakerService.getSpeakersByMeeting(id);
        return ResponseEntity.ok(speakers);
    }
}
