package com.aivideoassistant.service;

import com.aivideoassistant.dto.CreateMeetingRequest;
import com.aivideoassistant.dto.MeetingSummaryResponse;
import com.aivideoassistant.dto.StartPipelineResponse;
import com.aivideoassistant.exception.BadRequestException;
import com.aivideoassistant.exception.ResourceNotFoundException;
import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.repository.MeetingRepository;
import com.aivideoassistant.service.impl.MeetingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PipelineExecutionService pipelineExecutionService;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(meetingService, "projectRoot", ".");
    }

    @Test
    @DisplayName("createMeetingFromUrl: successfully creates meeting and starts pipeline")
    void createMeetingFromUrl_Success() {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .source("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .language("english")
                .build();

        Meeting savedMeeting = Meeting.builder()
                .id("meeting-123")
                .source(request.getSource())
                .language(request.getLanguage())
                .status("processing")
                .currentStage("downloading")
                .createdAt(Instant.now())
                .build();

        when(meetingRepository.save(any(Meeting.class))).thenReturn(savedMeeting);

        StartPipelineResponse response = meetingService.createMeetingFromUrl(request);

        assertNotNull(response);
        assertEquals("meeting-123", response.getMeetingId());
        assertEquals("processing", response.getStatus());

        verify(meetingRepository, times(1)).save(any(Meeting.class));
        verify(pipelineExecutionService, times(1)).runPipelineAsync(eq("meeting-123"), eq(request.getSource()), eq("english"));
    }

    @Test
    @DisplayName("createMeetingFromUrl: throws BadRequestException when source is empty")
    void createMeetingFromUrl_EmptySource_ThrowsException() {
        CreateMeetingRequest request = CreateMeetingRequest.builder().source("").build();

        assertThrows(BadRequestException.class, () -> meetingService.createMeetingFromUrl(request));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    @DisplayName("getAllMeetings: returns mapped list of summaries")
    void getAllMeetings_ReturnsList() {
        Meeting m1 = Meeting.builder().id("1").title("Meeting 1").status("completed").build();
        Meeting m2 = Meeting.builder().id("2").title("Meeting 2").status("processing").build();

        when(meetingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(m1, m2));

        List<MeetingSummaryResponse> results = meetingService.getAllMeetings();

        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("Meeting 1", results.get(0).getTitle());
    }

    @Test
    @DisplayName("getMeetingById: returns meeting when found")
    void getMeetingById_Found() {
        Meeting meeting = Meeting.builder().id("test-id").title("Architecture Discussion").build();
        when(meetingRepository.findById("test-id")).thenReturn(Optional.of(meeting));

        Meeting result = meetingService.getMeetingById("test-id");

        assertNotNull(result);
        assertEquals("Architecture Discussion", result.getTitle());
    }

    @Test
    @DisplayName("getMeetingById: throws ResourceNotFoundException when not found")
    void getMeetingById_NotFound_ThrowsException() {
        when(meetingRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> meetingService.getMeetingById("missing-id"));
    }

    @Test
    @DisplayName("deleteMeeting: successfully deletes when meeting exists")
    void deleteMeeting_Success() {
        when(meetingRepository.existsById("del-id")).thenReturn(true);

        meetingService.deleteMeeting("del-id");

        verify(meetingRepository, times(1)).deleteById("del-id");
    }

    @Test
    @DisplayName("deleteMeeting: throws ResourceNotFoundException when meeting does not exist")
    void deleteMeeting_NotFound_ThrowsException() {
        when(meetingRepository.existsById("del-id")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> meetingService.deleteMeeting("del-id"));
        verify(meetingRepository, never()).deleteById(anyString());
    }
}
