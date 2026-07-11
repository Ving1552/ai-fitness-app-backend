package com.fitness.activityservice.controllers;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.services.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {
    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(
            @RequestBody ActivityRequest activityRequest,
            @RequestHeader("X-User-Id") String userId) {
        activityRequest.setUserId(userId);
        return ResponseEntity.ok(activityService.trackActivity(activityRequest));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ActivityResponse>> getUserActivities(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(activityService.getUserActivities(userId, page, size));
    }
}
