package com.sanjuan.volunteer.controller;

import com.sanjuan.volunteer.common.ApiResponse;
import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.security.CurrentUser;
import com.sanjuan.volunteer.service.ActivityService;
import com.sanjuan.volunteer.service.AdminService;
import com.sanjuan.volunteer.service.ApplicationService;
import com.sanjuan.volunteer.service.AttendanceService;
import com.sanjuan.volunteer.service.OrganizerStatsService;
import com.sanjuan.volunteer.service.SummaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
public class OrganizerApiController {
    private final ActivityService activityService;
    private final ApplicationService applicationService;
    private final AttendanceService attendanceService;
    private final SummaryService summaryService;
    private final AdminService adminService;
    private final OrganizerStatsService organizerStatsService;

    public OrganizerApiController(ActivityService activityService, ApplicationService applicationService,
                                  AttendanceService attendanceService, SummaryService summaryService,
                                  AdminService adminService, OrganizerStatsService organizerStatsService) {
        this.activityService = activityService;
        this.applicationService = applicationService;
        this.attendanceService = attendanceService;
        this.summaryService = summaryService;
        this.adminService = adminService;
        this.organizerStatsService = organizerStatsService;
    }

    @GetMapping("/my-activities")
    public ApiResponse<List<Dtos.ActivityResponse>> myActivities(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(activityService.listByOrganizer(user.id()));
    }

    @PostMapping("/activities")
    public ApiResponse<Dtos.ActivityResponse> createActivity(@AuthenticationPrincipal CurrentUser user,
                                                             @RequestParam(defaultValue = "false") boolean submit,
                                                             @Valid @RequestBody Dtos.OrganizerActivitySaveRequest request) {
        return ApiResponse.ok(activityService.createByOrganizer(user.id(), request, submit));
    }

    @PutMapping("/activities/{id}")
    public ApiResponse<Dtos.ActivityResponse> updateActivity(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable Long id,
                                                           @RequestParam(defaultValue = "false") boolean submit,
                                                           @Valid @RequestBody Dtos.OrganizerActivitySaveRequest request) {
        return ApiResponse.ok(activityService.updateByOrganizer(user.id(), id, request, submit));
    }

    @PostMapping("/activities/{id}/submit")
    public ApiResponse<Dtos.ActivityResponse> submitActivity(@AuthenticationPrincipal CurrentUser user,
                                                             @PathVariable Long id) {
        return ApiResponse.ok(activityService.submitByOrganizer(user.id(), id));
    }

    @PostMapping("/activities/{id}/withdraw")
    public ApiResponse<Dtos.ActivityResponse> withdrawActivity(@AuthenticationPrincipal CurrentUser user,
                                                               @PathVariable Long id) {
        return ApiResponse.ok(activityService.withdrawByOrganizer(user.id(), id));
    }

    @GetMapping("/activities/{id}/applications")
    public ApiResponse<List<Dtos.ApplicationResponse>> applications(@AuthenticationPrincipal CurrentUser user,
                                                                    @PathVariable Long id) {
        activityService.ensureOrganizerOwns(user.id(), id);
        return ApiResponse.ok(applicationService.listByActivity(id));
    }

    @PostMapping("/applications/audit")
    public ApiResponse<Dtos.ApplicationResponse> auditApplication(@AuthenticationPrincipal CurrentUser user,
                                                                @Valid @RequestBody Dtos.ApplicationAuditRequest request) {
        return ApiResponse.ok(applicationService.auditByOrganizer(
                user.id(), request.applicationId(), request.status()));
    }

    @PostMapping("/activities/{id}/participants")
    public ApiResponse<Dtos.ApplicationResponse> addParticipant(@AuthenticationPrincipal CurrentUser user,
                                                                  @PathVariable Long id,
                                                                  @Valid @RequestBody Dtos.AddParticipantRequest request) {
        Dtos.AddParticipantRequest req = new Dtos.AddParticipantRequest(id, request.studentNo());
        return ApiResponse.ok(applicationService.addParticipant(user.id(), Enums.Role.ORGANIZER, req));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteDraft(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        activityService.deleteDraftByOrganizer(user.id(), id);
        return ApiResponse.ok("草稿活动已删除", null);
    }

    @PostMapping("/attendance/confirm-check-in")
    public ApiResponse<Dtos.AttendanceResponse> confirmCheckIn(@AuthenticationPrincipal CurrentUser user,
                                                               @Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.confirmCheckIn(user.id(), request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/confirm-check-out")
    public ApiResponse<Dtos.AttendanceResponse> confirmCheckOut(@AuthenticationPrincipal CurrentUser user,
                                                                @Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.confirmCheckOut(user.id(), request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/mark-absent")
    public ApiResponse<Dtos.AttendanceResponse> markAbsent(@AuthenticationPrincipal CurrentUser user,
                                                         @Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.markAbsent(user.id(), request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/proxy-check-in")
    public ApiResponse<Dtos.AttendanceResponse> proxyCheckIn(@AuthenticationPrincipal CurrentUser user,
                                                             @Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.proxyCheckIn(user.id(), request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/proxy-check-out")
    public ApiResponse<Dtos.AttendanceResponse> proxyCheckOut(@AuthenticationPrincipal CurrentUser user,
                                                              @Valid @RequestBody Dtos.OrganizerAttendanceActionRequest request) {
        return ApiResponse.ok(attendanceService.proxyCheckOut(user.id(), request.activityId(), request.studentNo()));
    }

    @PostMapping("/attendance/assist-check")
    public ApiResponse<Dtos.AttendanceResponse> assistCheck(@AuthenticationPrincipal CurrentUser user,
                                                            @Valid @RequestBody Dtos.AssistAttendanceRequest request) {
        return ApiResponse.ok(attendanceService.assistCheck(user.id(), request.activityId(), request.studentNo(), request.checkIn()));
    }

    @PutMapping("/attendance/{id}/evaluate")
    public ApiResponse<Dtos.AttendanceResponse> evaluate(@AuthenticationPrincipal CurrentUser user,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody Dtos.EvaluateRequest request) {
        return ApiResponse.ok(attendanceService.evaluate(user.id(), id, request.evaluation()));
    }

    @PostMapping("/activities/{id}/summary")
    public ApiResponse<Dtos.SummaryResponse> submitSummary(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody Dtos.SummaryRequest request) {
        return ApiResponse.ok(summaryService.submit(user.id(), id, request));
    }

    @PutMapping("/activities/{id}/summary")
    public ApiResponse<Dtos.SummaryResponse> updateSummary(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody Dtos.SummaryRequest request) {
        return ApiResponse.ok(summaryService.update(user.id(), id, request));
    }

    @GetMapping("/summary-activities")
    public ApiResponse<List<Dtos.SummaryActivityOption>> summaryActivities(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(summaryService.listSummaryActivities(user.id()));
    }

    @GetMapping("/summaries")
    public ApiResponse<List<Dtos.SummaryListItem>> summaries(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(summaryService.listByOrganizer(user.id()));
    }

    @GetMapping("/summaries/{id}")
    public ApiResponse<Dtos.SummaryResponse> summaryDetail(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable Long id) {
        return ApiResponse.ok(summaryService.getDetail(user.id(), id));
    }

    @DeleteMapping("/summaries/{id}")
    public ApiResponse<Void> deleteSummary(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        summaryService.delete(user.id(), id);
        return ApiResponse.ok("活动总结已删除", null);
    }

    @GetMapping("/stats/activity-count")
    public ApiResponse<Dtos.OrganizerStatCountResponse> statActivityCount(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(organizerStatsService.countActivities(user.id()));
    }

    @GetMapping("/stats/volunteer-count")
    public ApiResponse<Dtos.OrganizerStatCountResponse> statVolunteerCount(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(organizerStatsService.countParticipatingVolunteers(user.id()));
    }

    @GetMapping("/stats/service-hours")
    public ApiResponse<Dtos.OrganizerServiceHoursResponse> statServiceHours(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(organizerStatsService.sumServiceHours(user.id()));
    }

    @GetMapping("/stats/pending-check-in-count")
    public ApiResponse<Dtos.OrganizerStatCountResponse> statPendingCheckIn(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(organizerStatsService.countPendingCheckIn(user.id()));
    }

    @PutMapping("/profile/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                                            @Valid @RequestBody Dtos.ChangePasswordRequest request) {
        adminService.changeMyPassword(user.id(), request);
        return ApiResponse.ok("密码修改成功", null);
    }
}
