package com.sanjuan.volunteer.controller;

import com.sanjuan.volunteer.common.ApiResponse;
import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.security.CurrentUser;
import com.sanjuan.volunteer.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {
    private final AdminService adminService;
    private final ActivityService activityService;
    private final ApplicationService applicationService;
    private final AttendanceService attendanceService;

    public AdminApiController(AdminService adminService, ActivityService activityService,
                              ApplicationService applicationService, AttendanceService attendanceService) {
        this.adminService = adminService;
        this.activityService = activityService;
        this.applicationService = applicationService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/volunteers")
    public ApiResponse<List<Dtos.VolunteerResponse>> volunteers() {
        return ApiResponse.ok(adminService.listVolunteers());
    }

    @PostMapping("/volunteers")
    public ApiResponse<Dtos.VolunteerResponse> createVolunteer(@Valid @RequestBody Dtos.VolunteerCreateRequest request) {
        return ApiResponse.ok(adminService.createVolunteer(request));
    }

    @PostMapping("/volunteers/audit")
    public ApiResponse<Void> auditVolunteer(@Valid @RequestBody Dtos.VolunteerAuditRequest request) {
        adminService.auditVolunteer(request.userId(), request.status());
        return ApiResponse.ok(null);
    }

    @PutMapping("/volunteers/{id}")
    public ApiResponse<Dtos.VolunteerResponse> updateVolunteer(@PathVariable Long id,
                                                               @Valid @RequestBody Dtos.VolunteerAdminUpdateRequest request) {
        return ApiResponse.ok(adminService.updateVolunteer(id, request));
    }

    @DeleteMapping("/volunteers/{id}")
    public ApiResponse<Void> deleteVolunteer(@PathVariable Long id) {
        adminService.deleteVolunteer(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/organizers")
    public ApiResponse<List<Dtos.OrganizerResponse>> organizers() {
        return ApiResponse.ok(adminService.listOrganizers());
    }

    @PostMapping("/organizers")
    public ApiResponse<Dtos.OrganizerResponse> createOrganizer(@Valid @RequestBody Dtos.OrganizerSaveRequest request) {
        return ApiResponse.ok(adminService.createOrganizer(request));
    }

    @PutMapping("/organizers/{id}")
    public ApiResponse<Dtos.OrganizerResponse> updateOrganizer(@PathVariable Long id,
                                                             @Valid @RequestBody Dtos.OrganizerSaveRequest request) {
        return ApiResponse.ok(adminService.updateOrganizer(id, request));
    }

    @PostMapping("/organizers/audit")
    public ApiResponse<Void> auditOrganizer(@Valid @RequestBody Dtos.OrganizerAuditRequest request) {
        adminService.auditOrganizer(request.userId(), request.status());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/organizers/{id}")
    public ApiResponse<Void> deleteOrganizer(@PathVariable Long id) {
        adminService.deleteOrganizer(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/activities")
    public ApiResponse<Page<Dtos.ActivityResponse>> listActivities(Pageable pageable) {
        return ApiResponse.ok(activityService.listAll(pageable));
    }

    @PostMapping("/activities")
    public ApiResponse<Dtos.ActivityResponse> createActivity(@Valid @RequestBody Dtos.ActivityCreateRequest request) {
        return ApiResponse.ok(activityService.create(request));
    }

    @GetMapping("/activities/{id}")
    public ApiResponse<Dtos.ActivityResponse> getActivity(@PathVariable Long id) {
        return ApiResponse.ok(activityService.getActivitySynced(id));
    }

    @PutMapping("/activities/{id}")
    public ApiResponse<Dtos.ActivityResponse> updateActivity(@PathVariable Long id,
                                                             @Valid @RequestBody Dtos.ActivityUpdateRequest request) {
        return ApiResponse.ok(activityService.updateByAdmin(id, request));
    }

    @PostMapping("/activities/{id}/publish")
    public ApiResponse<Dtos.ActivityResponse> publishActivity(@PathVariable Long id) {
        return ApiResponse.ok(activityService.publishByAdmin(id));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteByAdmin(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/activities/{id}/applications")
    public ApiResponse<List<Dtos.ApplicationResponse>> activityApplications(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.listByActivity(id));
    }

    @PostMapping("/activities/applications/audit")
    public ApiResponse<Dtos.ApplicationResponse> auditApplication(@Valid @RequestBody Dtos.ApplicationAuditRequest request) {
        return ApiResponse.ok(applicationService.audit(request.applicationId(), request.status()));
    }

    @PostMapping("/activities/{id}/participants")
    public ApiResponse<Dtos.ApplicationResponse> addParticipant(@PathVariable Long id,
                                                                  @Valid @RequestBody Dtos.AddParticipantRequest request) {
        Dtos.AddParticipantRequest req = new Dtos.AddParticipantRequest(id, request.studentNo());
        return ApiResponse.ok(applicationService.addParticipant(null, Enums.Role.ADMIN, req));
    }

    @PostMapping("/attendance/confirm-check-in")
    public ApiResponse<Dtos.AttendanceResponse> confirmCheckIn(@Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.confirmCheckInByAdmin(request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/confirm-check-out")
    public ApiResponse<Dtos.AttendanceResponse> confirmCheckOut(@Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.confirmCheckOutByAdmin(request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/mark-absent")
    public ApiResponse<Dtos.AttendanceResponse> markAbsent(@Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.markAbsentByAdmin(request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/proxy-check-in")
    public ApiResponse<Dtos.AttendanceResponse> proxyCheckIn(@Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.proxyCheckInByAdmin(request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/proxy-check-out")
    public ApiResponse<Dtos.AttendanceResponse> proxyCheckOut(@Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.proxyCheckOutByAdmin(request.activityId(), request.studentNo()));
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<Dtos.DashboardStatsResponse> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/dashboard/stats/export")
    public void exportStats(HttpServletResponse response) throws IOException {
        Dtos.DashboardStatsResponse stats = adminService.stats();
        String csv = "指标,数值\n" +
                "志愿者总数," + stats.volunteerCount() + "\n" +
                "活动总数," + stats.activityCount() + "\n" +
                "累计服务时长," + stats.totalServiceHours() + "\n" +
                "报名总数," + stats.applicationCount() + "\n" +
                "审核通过数," + stats.approvedCount() + "\n" +
                "报名通过率," + stats.approvalRate() + "%\n";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=dashboard-stats.csv");
        response.getWriter().write('\ufeff');
        response.getWriter().write(csv);
    }

    @PutMapping("/profile/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                                            @Valid @RequestBody Dtos.ChangePasswordRequest request) {
        adminService.changeMyPassword(user.id(), request);
        return ApiResponse.ok("密码修改成功", null);
    }
}
