package com.aivideoassistant.controller;

import com.aivideoassistant.dto.CreateMeetingRequest;
import com.aivideoassistant.dto.MeetingSummaryResponse;
import com.aivideoassistant.dto.StartPipelineResponse;
import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.service.MeetingService;
import com.aivideoassistant.service.MeetingSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private MeetingSseService meetingSseService;

    @Test
    @DisplayName("POST /api/meetings: returns 201 Created with meetingId")
    void createMeeting_ReturnsCreated() throws Exception {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .source("https://www.youtube.com/watch?v=sample")
                .language("english")
                .build();

        StartPipelineResponse response = StartPipelineResponse.builder()
                .meetingId("meeting-456")
                .status("processing")
                .message("Pipeline started. Connect to SSE for progress.")
                .build();

        when(meetingService.createMeetingFromUrl(any(CreateMeetingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").value("meeting-456"))
                .andExpect(jsonPath("$.status").value("processing"));
    }

    @Test
    @DisplayName("GET /api/meetings: returns 200 OK with list of summaries")
    void getAllMeetings_ReturnsList() throws Exception {
        MeetingSummaryResponse summary = MeetingSummaryResponse.builder()
                .id("meeting-789")
                .title("Weekly Sync")
                .status("completed")
                .createdAt(Instant.now())
                .build();

        when(meetingService.getAllMeetings()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("meeting-789"))
                .andExpect(jsonPath("$[0].title").value("Weekly Sync"));
    }

    @Test
    @DisplayName("GET /api/meetings/{id}: returns 200 OK with meeting object")
    void getMeetingById_ReturnsMeeting() throws Exception {
        Meeting meeting = Meeting.builder()
                .id("meeting-789")
                .title("Weekly Sync")
                .status("completed")
                .transcript("Hello everyone...")
                .build();

        when(meetingService.getMeetingById("meeting-789")).thenReturn(meeting);

        mockMvc.perform(get("/api/meetings/meeting-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("meeting-789"))
                .andExpect(jsonPath("$._id").value("meeting-789"))
                .andExpect(jsonPath("$.transcript").value("Hello everyone..."));
    }

    @Test
    @DisplayName("DELETE /api/meetings/{id}: returns 200 OK with deletion message")
    void deleteMeeting_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/meetings/meeting-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Meeting deleted"));
    }
}
