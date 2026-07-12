package com.fitness.activityservice.services;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.models.Activity;
import com.fitness.activityservice.models.ActivityType;
import com.fitness.activityservice.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private KafkaTemplate<String, Activity> kafkaTemplate;

    @InjectMocks
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activityService, "topicName", "activity-events");
    }

    // ── Helper builders ──────────────────────────────────────────────────────

    private ActivityRequest buildRequest(String userId) {
        ActivityRequest request = new ActivityRequest();
        request.setUserId(userId);
        request.setType(ActivityType.RUNNING);
        request.setDuration(30);
        request.setCaloriesBurned(300);
        request.setStartTime(LocalDateTime.now());
        return request;
    }

    private Activity buildSavedActivity(String userId) {
        return Activity.builder()
                .id("activity-123")
                .userId(userId)
                .type(ActivityType.RUNNING)
                .duration(30)
                .caloriesBurned(300)
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void trackActivity_validUser_savesActivityAndPublishesToKafka() {
        // Arrange
        String userId = "user-1";
        ActivityRequest request = buildRequest(userId);
        Activity savedActivity = buildSavedActivity(userId);

        when(userValidationService.validateUser(userId)).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Act
        ActivityResponse response = activityService.trackActivity(request);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(ActivityType.RUNNING, response.getType());
        assertEquals(30, response.getDuration());
        assertEquals(300, response.getCaloriesBurned());

        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(kafkaTemplate, times(1)).send(eq("activity-events"), eq(userId), any(Activity.class));
    }

    @Test
    void trackActivity_invalidUser_throwsRuntimeException() {
        // Arrange
        String userId = "invalid-user";
        ActivityRequest request = buildRequest(userId);

        when(userValidationService.validateUser(userId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> activityService.trackActivity(request));

        assertTrue(exception.getMessage().contains("Invalid user"));
        verify(activityRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void trackActivity_validUser_responseMatchesSavedActivity() {
        // Arrange
        String userId = "user-2";
        ActivityRequest request = buildRequest(userId);
        Activity savedActivity = buildSavedActivity(userId);

        when(userValidationService.validateUser(userId)).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Act
        ActivityResponse response = activityService.trackActivity(request);

        // Assert
        assertEquals(savedActivity.getId(), response.getId());
        assertEquals(savedActivity.getUserId(), response.getUserId());
        assertEquals(savedActivity.getType(), response.getType());
        assertEquals(savedActivity.getDuration(), response.getDuration());
        assertEquals(savedActivity.getCaloriesBurned(), response.getCaloriesBurned());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void trackActivity_kafkaFailure_doesNotPropagateException() {
        // Arrange
        String userId = "user-3";
        ActivityRequest request = buildRequest(userId);
        Activity savedActivity = buildSavedActivity(userId);

        when(userValidationService.validateUser(userId)).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(kafkaTemplate).send(anyString(), anyString(), any(Activity.class));

        // Act & Assert — should not throw because Kafka is wrapped in try/catch
        ActivityResponse response = activityService.trackActivity(request);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void getUserActivities_returnsPaginatedResults() {
        // Arrange
        String userId = "user-4";
        Activity activity = buildSavedActivity(userId);
        Page<Activity> activityPage = new PageImpl<>(List.of(activity),
                PageRequest.of(0, 10, Sort.by("startTime").descending()), 1);

        when(activityRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(activityPage);

        // Act
        Page<ActivityResponse> result = activityService.getUserActivities(userId, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(activityRepository, times(1)).findByUserId(eq(userId), any(Pageable.class));
    }
}