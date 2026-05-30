package com.sanjuan.volunteer.dto;

import com.sanjuan.volunteer.entity.Enums;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank String username, @NotBlank @Size(min = 6) String password,
                                  @NotBlank String realName, Integer age, String skills, String preferences) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, Long userId, String username, Enums.Role role) {}

    public record ActivityCreateRequest(@NotBlank String title, @NotBlank String description, String requirements,
                                        String imageUrl, @NotNull Long organizerId,
                                        @NotNull @Min(1) Integer maxVolunteers,
                                        @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {}
    public record ActivityUpdateRequest(String title, String description, String requirements, String imageUrl,
                                        Enums.ActivityStatus status, Integer maxVolunteers,
                                        LocalDateTime startTime, LocalDateTime endTime) {}
    public record OrganizerActivitySaveRequest(@NotBlank String title, @NotBlank String description, String requirements,
                                               String imageUrl, @NotNull @Min(1) Integer maxVolunteers,
                                               @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {}
    public record ActivityResponse(Long id, String title, String description, String requirements, String imageUrl,
                                   Enums.ActivityStatus status, Long organizerId, Integer maxVolunteers,
                                   Long applicationCount, Long approvedCount,
                                   LocalDateTime startTime, LocalDateTime endTime) {}

    public record ApplicationRequest(@NotNull Long activityId) {}
    public record ApplicationAuditRequest(@NotNull Long applicationId, @NotNull Enums.ApplicationStatus status) {}
    public record AddParticipantRequest(@NotNull Long activityId, @NotBlank String studentNo) {}
    public record OrganizerAttendanceActionRequest(@NotNull Long activityId, @NotBlank String studentNo) {}
    public record ApplicationResponse(Long id, Long activityId, String activityTitle,
                                      String studentNo, String volunteerName,
                                      Enums.ApplicationStatus status, LocalDateTime applyTime,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                      Boolean checkInRequested, Boolean checkOutRequested,
                                      BigDecimal serviceHours) {}

    public record AttendanceRequest(@NotNull Long activityId) {}
    public record AssistAttendanceRequest(@NotNull Long activityId, @NotBlank String studentNo, @NotNull Boolean checkIn) {}
    public record EvaluateRequest(@NotBlank String evaluation) {}
    public record AttendanceResponse(Long id, Long activityId, String studentNo, LocalDateTime checkInTime,
                                     LocalDateTime checkOutTime, Boolean checkInRequested, Boolean checkOutRequested,
                                     BigDecimal hours, String evaluation) {}

    public record ProfileUpdateRequest(String realName, Integer age, String phone, String skills, String preferences) {}
    public record ProfileResponse(Long userId, String username, String realName, Integer age, String skills,
                                  String preferences, BigDecimal totalHours, List<AttendanceResponse> records) {}

    public record VolunteerCreateRequest(@NotBlank String username, String password, @NotBlank String realName,
                                         Integer age, String phone, String skills, String preferences,
                                         Enums.UserStatus status) {}
    public record VolunteerAdminUpdateRequest(String username, Enums.UserStatus status, String realName,
                                              Integer age, String phone, String skills, String preferences) {}
    public record VolunteerAuditRequest(@NotNull Long userId, @NotNull Enums.UserStatus status) {}
    public record VolunteerResponse(Long userId, String username, Enums.UserStatus status, String realName,
                                    Integer age, String phone, String skills, String preferences, BigDecimal totalHours) {}

    public record OrganizerSaveRequest(@NotBlank String username, String password, @NotBlank String realName,
                                       String phone, String department, Enums.UserStatus status) {}
    public record OrganizerAuditRequest(@NotNull Long userId, @NotNull Enums.UserStatus status) {}
    public record OrganizerResponse(Long userId, String username, Enums.UserStatus status, String realName,
                                    String phone, String department, long activityCount) {}

    public record DashboardStatsResponse(long volunteerCount, long activityCount, BigDecimal totalServiceHours,
                                         long applicationCount, long approvedCount, BigDecimal approvalRate,
                                         long publishedActivities, long pendingApplications) {}

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 6) String newPassword) {}

    public record SummaryRequest(@NotBlank String content, String feedback, List<String> imageUrls) {}
    public record SummaryListItem(Long id, Long activityId, String activityTitle,
                                  LocalDateTime submitTime, LocalDateTime updateTime, String status) {}
    public record SummaryActivityOption(Long activityId, String activityTitle, boolean summarized) {}
    public record SummaryResponse(Long id, Long activityId, String activityTitle, Long organizerId,
                                  String content, String feedback, LocalDateTime submitTime, LocalDateTime updateTime,
                                  boolean adminViewed, List<String> imageUrls) {}

    public record OrganizerStatCountResponse(long count) {}
    public record OrganizerServiceHoursResponse(BigDecimal hours) {}

    public record ForumPostCreateRequest(@NotBlank String title, @NotBlank String content, Long activityId,
                                         List<String> imageUrls) {}
    public record ForumNoticeCreateRequest(@NotBlank String title, @NotBlank String content, @NotNull Long activityId,
                                           List<String> imageUrls) {}
    public record ForumPostUpdateRequest(String title, String content, Long activityId, List<String> imageUrls) {}
    public record ForumCommentRequest(@NotBlank String content) {}
    public record ForumAdminUpdateRequest(Boolean hidden, Boolean topped) {}
    public record ForumActivityOption(Long activityId, String activityTitle) {}
    public record ForumCommentResponse(Long id, Long authorId, String authorName, String content, LocalDateTime createTime) {}
    public record ForumPostSummary(Long id, String title, String preview, String authorName, Long authorId,
                                   LocalDateTime createTime, int viewCount, int likeCount, int commentCount,
                                   Long activityId, String activityTitle, String postType,
                                   boolean hidden, boolean topped, boolean deleted,
                                   boolean canEdit, boolean canDelete, boolean canHide) {}
    public record ForumPostDetail(Long id, String title, String content, String authorName, Long authorId,
                                LocalDateTime createTime, int viewCount, int likeCount, boolean liked,
                                Long activityId, String activityTitle, String postType,
                                boolean hidden, boolean topped, boolean deleted,
                                boolean canEdit, boolean canDelete, boolean canHide,
                                List<String> imageUrls, List<ForumCommentResponse> comments) {}
    public record UploadResponse(String url) {}
}
