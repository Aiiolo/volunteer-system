package com.sanjuan.volunteer.controller;

import com.sanjuan.volunteer.common.ApiResponse;
import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.security.CurrentUser;
import com.sanjuan.volunteer.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/volunteer")
public class VolunteerApiController {

    private final AuthService authService;
    private final ActivityService activityService;
    private final ApplicationService applicationService;
    private final AttendanceService attendanceService;
    private final ProfileService profileService;

    public VolunteerApiController(
            AuthService authService,
            ActivityService activityService,
            ApplicationService applicationService,
            AttendanceService attendanceService,
            ProfileService profileService) {
        this.authService = authService;
        this.activityService = activityService;
        this.applicationService = applicationService;
        this.attendanceService = attendanceService;
        this.profileService = profileService;
    }

    @PostMapping("/auth/register")
    public ApiResponse<Void> register(@Valid @RequestBody Dtos.RegisterRequest request) {
        authService.registerVolunteer(request);
        return ApiResponse.ok("注册成功，等待管理员审核", null);
    }

    @PostMapping("/auth/login")
    public ApiResponse<Dtos.LoginResponse> login(@Valid @RequestBody Dtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/activities")
    public ApiResponse<Page<Dtos.ActivityResponse>> activities(Pageable pageable) {
        return ApiResponse.ok(activityService.listPublished(pageable));
    }

    @GetMapping("/activities/latest")
    public ApiResponse<List<Dtos.ActivityResponse>> latestActivities() {
        return ApiResponse.ok(activityService.listLatestPublished(3));
    }

    @PostMapping("/applications")
    public ApiResponse<Dtos.ApplicationResponse> apply(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody Dtos.ApplicationRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(applicationService.apply(user.id(), request.activityId()));
    }

    @GetMapping("/applications/status")
    public ApiResponse<List<Dtos.ApplicationResponse>> applicationStatus(
            @AuthenticationPrincipal CurrentUser user) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(applicationService.listByVolunteer(user.id()));
    }

    @GetMapping("/activities/my")
    public ApiResponse<List<Dtos.ApplicationResponse>> myActivities(
            @AuthenticationPrincipal CurrentUser user) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(applicationService.listByVolunteer(user.id()));
    }

    @DeleteMapping("/applications/{id}")
    public ApiResponse<Void> cancelApplication(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable Long id) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        applicationService.cancelByVolunteer(user.id(), id);
        return ApiResponse.ok("已取消报名", null);
    }

    @PostMapping("/attendance/check-in")
    public ApiResponse<Dtos.AttendanceResponse> checkIn(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody Dtos.AttendanceRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(attendanceService.requestCheckIn(user.id(), request.activityId()));
    }

    @PostMapping("/attendance/check-out")
    public ApiResponse<Dtos.AttendanceResponse> checkOut(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody Dtos.AttendanceRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(attendanceService.requestCheckOut(user.id(), request.activityId()));
    }

    @GetMapping("/profile")
    public ApiResponse<Dtos.ProfileResponse> profile(
            @AuthenticationPrincipal CurrentUser user) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(profileService.getProfile(user.id()));
    }

    @PutMapping("/profile")
    public ApiResponse<Dtos.ProfileResponse> updateProfile(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody Dtos.ProfileUpdateRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        return ApiResponse.ok(profileService.update(user.id(), request));
    }

    @PutMapping("/profile/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody Dtos.ChangePasswordRequest request) {
        if (user == null || user.id() == null) {
            return ApiResponse.fail("未登录或登录已过期");
        }
        authService.changePassword(user.id(), request);
        return ApiResponse.ok("密码修改成功", null);
    }
}