package com.aivideoassistant.repository;

import com.aivideoassistant.model.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends MongoRepository<Meeting, String> {
    List<Meeting> findAllByOrderByCreatedAtDesc();
}
