package com.aivideoassistant;

import com.aivideoassistant.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ServerSpringbootApplicationTests {

    @MockBean
    private MeetingRepository meetingRepository;

    @Test
    void contextLoads() {
    }
}
