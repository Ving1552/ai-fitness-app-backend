package com.fitness.activityservice.repository;

import com.fitness.activityservice.models.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {
    Page<Activity> findByUserId(String userId, Pageable pageable);
}
