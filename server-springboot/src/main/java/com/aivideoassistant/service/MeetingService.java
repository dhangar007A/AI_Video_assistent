package com.aivideoassistant.service;

import com.aivideoassistant.dto.CreateMeetingRequest;
import com.aivideoassistant.dto.MeetingSummaryResponse;
import com.aivideoassistant.dto.StartPipelineResponse;
import com.aivideoassistant.model.Meeting;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MeetingService {
    StartPipelineResponse createMeetingFromUrl(CreateMeetingRequest request);
    StartPipelineResponse createMeetingFromFile(MultipartFile file, String language);
    List<MeetingSummaryResponse> getAllMeetings();
    Meeting getMeetingById(String id);
    void deleteMeeting(String id);
}
