package com.meeting.assistant.repository;

import com.meeting.assistant.entity.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeakerRepository extends JpaRepository<Speaker, Long> {

    List<Speaker> findByMeetingId(Long meetingId);
}
