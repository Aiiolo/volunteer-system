package com.sanjuan.volunteer.controller;

import com.sanjuan.volunteer.common.ApiResponse;
import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.service.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityApiController {
    private final ActivityService activityService;

    public ActivityApiController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/latest")
    public ApiResponse<List<Dtos.ActivityResponse>> latest() {
        return ApiResponse.ok(activityService.listLatestPublished(3));
    }

    @GetMapping("/{id}")
    public ApiResponse<Dtos.ActivityResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.getActivitySynced(id));
    }
}
