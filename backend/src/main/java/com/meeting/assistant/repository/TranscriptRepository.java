package com.meeting.assistant.repository;

import com.meeting.assistant.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    List<Transcript> findByMeetingIdOrderBySequenceOrderAsc(Long meetingId);

    List<Transcript> findByMeetingIdOrderBySequenceOrderDesc(Long meetingId);

    Integer countByMeetingId(Long meetingId);
}
