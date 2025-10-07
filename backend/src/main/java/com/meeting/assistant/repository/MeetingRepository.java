package com.meeting.assistant.repository;

import com.meeting.assistant.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByOrderByStartTimeDesc();

    List<Meeting> findByStatus(Meeting.MeetingStatus status);
}
