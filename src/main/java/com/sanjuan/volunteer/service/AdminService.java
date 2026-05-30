package com.sanjuan.volunteer.service;

import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.entity.OrganizerProfile;
import com.sanjuan.volunteer.entity.SysUser;
import com.sanjuan.volunteer.entity.VolunteerProfile;
import com.sanjuan.volunteer.exception.BusinessException;
import com.sanjuan.volunteer.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AdminService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserRepository userRepository;
    private final VolunteerProfileRepository profileRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final ActivityRepository activityRepository;
    private final ActivityApplicationRepository applicationRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(SysUserRepository userRepository, VolunteerProfileRepository profileRepository,
                        OrganizerProfileRepository organizerProfileRepository,
                        ActivityRepository activityRepository, ActivityApplicationRepository applicationRepository,
                        AttendanceRecordRepository attendanceRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.organizerProfileRepository = organizerProfileRepository;
        this.activityRepository = activityRepository;
        this.applicationRepository = applicationRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Dtos.VolunteerResponse> listVolunteers() {
        return userRepository.findByRole(Enums.Role.VOLUNTEER).stream().map(this::toVolunteerResponse).toList();
    }

    @Transactional
    public Dtos.VolunteerResponse createVolunteer(Dtos.VolunteerCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("学号/账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        String pwd = request.password() != null && !request.password().isBlank() ? request.password() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRole(Enums.Role.VOLUNTEER);
        user.setStatus(request.status() != null ? request.status() : Enums.UserStatus.ACTIVE);
        userRepository.save(user);

        VolunteerProfile profile = new VolunteerProfile();
        profile.setUser(user);
        profile.setRealName(request.realName().trim());
        profile.setAge(request.age());
        profile.setPhone(trimOrNull(request.phone()));
        profile.setSkills(request.skills());
        profile.setPreferences(request.preferences());
        profileRepository.save(profile);
        return toVolunteerResponse(user);
    }

    @Transactional
    public void auditVolunteer(Long userId, Enums.UserStatus status) {
        SysUser user = requireVolunteer(userId);
        if (status != Enums.UserStatus.ACTIVE && status != Enums.UserStatus.DISABLED && status != Enums.UserStatus.PENDING) {
            throw new BusinessException("无效状态");
        }
        user.setStatus(status);
    }

    @Transactional
    public Dtos.VolunteerResponse updateVolunteer(Long userId, Dtos.VolunteerAdminUpdateRequest request) {
        SysUser user = requireVolunteer(userId);
        VolunteerProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("志愿者档案不存在"));

        if (request.username() != null && !request.username().isBlank()) {
            String username = request.username().trim();
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new BusinessException("学号/账号已存在");
            }
            user.setUsername(username);
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.realName() != null) profile.setRealName(request.realName().trim());
        if (request.age() != null) profile.setAge(request.age());
        if (request.phone() != null) profile.setPhone(trimOrNull(request.phone()));
        if (request.skills() != null) profile.setSkills(request.skills());
        if (request.preferences() != null) profile.setPreferences(request.preferences());
        return toVolunteerResponse(user);
    }

    @Transactional
    public void deleteVolunteer(Long userId) {
        SysUser user = requireVolunteer(userId);
        profileRepository.deleteById(userId);
        userRepository.delete(user);
    }

    public List<Dtos.OrganizerResponse> listOrganizers() {
        return userRepository.findByRole(Enums.Role.ORGANIZER).stream().map(this::toOrganizerResponse).toList();
    }

    @Transactional
    public Dtos.OrganizerResponse createOrganizer(Dtos.OrganizerSaveRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        String pwd = request.password() != null && !request.password().isBlank() ? request.password() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRole(Enums.Role.ORGANIZER);
        user.setStatus(request.status() != null ? request.status() : Enums.UserStatus.ACTIVE);
        userRepository.save(user);

        OrganizerProfile profile = new OrganizerProfile();
        profile.setUser(user);
        profile.setRealName(request.realName().trim());
        profile.setPhone(trimOrNull(request.phone()));
        profile.setDepartment(trimOrNull(request.department()));
        organizerProfileRepository.save(profile);
        return toOrganizerResponse(user);
    }

    @Transactional
    public Dtos.OrganizerResponse updateOrganizer(Long userId, Dtos.OrganizerSaveRequest request) {
        SysUser user = requireOrganizer(userId);
        OrganizerProfile profile = getOrCreateOrganizerProfile(user);

        if (request.username() != null && !request.username().isBlank()) {
            String username = request.username().trim();
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new BusinessException("账号已存在");
            }
            user.setUsername(username);
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.realName() != null) profile.setRealName(request.realName().trim());
        if (request.phone() != null) profile.setPhone(trimOrNull(request.phone()));
        if (request.department() != null) profile.setDepartment(trimOrNull(request.department()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return toOrganizerResponse(user);
    }

    @Transactional
    public void auditOrganizer(Long userId, Enums.UserStatus status) {
        SysUser user = requireOrganizer(userId);
        if (status != Enums.UserStatus.ACTIVE && status != Enums.UserStatus.DISABLED) {
            throw new BusinessException("活动负责人仅支持正常或禁用状态");
        }
        user.setStatus(status);
    }

    @Transactional
    public void deleteOrganizer(Long userId) {
        requireOrganizer(userId);
        long count = activityRepository.countByOrganizerId(userId);
        if (count > 0) {
            throw new BusinessException("该负责人名下仍有 " + count + " 个活动，无法删除");
        }
        organizerProfileRepository.findById(userId).ifPresent(organizerProfileRepository::delete);
        userRepository.deleteById(userId);
    }

    public Dtos.DashboardStatsResponse stats() {
        long volunteers = userRepository.findByRole(Enums.Role.VOLUNTEER).size();
        long activities = activityRepository.count();
        long publishedActivities = activityRepository.countByStatusIn(List.of(
                Enums.ActivityStatus.PUBLISHED,
                Enums.ActivityStatus.ONGOING,
                Enums.ActivityStatus.COMPLETED));
        long applications = applicationRepository.count();
        long approved = applicationRepository.countByStatus(Enums.ApplicationStatus.WAIT_CHECK_IN)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.WAIT_CHECK_IN_CONFIRM)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.WAIT_CHECK_OUT)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.WAIT_CHECK_OUT_CONFIRM)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.COMPLETED)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.ABSENT)
                + applicationRepository.countByStatus(Enums.ApplicationStatus.APPROVED);
        long pendingApplications = applicationRepository.countByStatus(Enums.ApplicationStatus.PENDING);
        BigDecimal rate = applications == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(approved)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(applications), 2, RoundingMode.HALF_UP);
        return new Dtos.DashboardStatsResponse(
                volunteers,
                activities,
                attendanceRepository.sumAllHours(),
                applications,
                approved,
                rate,
                publishedActivities,
                pendingApplications);
    }

    @Transactional
    public void changeMyPassword(Long userId, Dtos.ChangePasswordRequest request) {
        SysUser user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    private SysUser requireVolunteer(Long userId) {
        SysUser user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getRole() != Enums.Role.VOLUNTEER) {
            throw new BusinessException("只能操作志愿者账号");
        }
        return user;
    }

    private SysUser requireOrganizer(Long userId) {
        SysUser user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getRole() != Enums.Role.ORGANIZER) {
            throw new BusinessException("只能操作活动负责人账号");
        }
        return user;
    }

    private OrganizerProfile getOrCreateOrganizerProfile(SysUser user) {
        return organizerProfileRepository.findById(user.getId()).orElseGet(() -> {
            OrganizerProfile profile = new OrganizerProfile();
            profile.setUser(user);
            profile.setRealName(user.getUsername());
            return organizerProfileRepository.save(profile);
        });
    }

    private Dtos.VolunteerResponse toVolunteerResponse(SysUser user) {
        VolunteerProfile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("志愿者档案不存在"));
        return new Dtos.VolunteerResponse(user.getId(), user.getUsername(), user.getStatus(), profile.getRealName(),
                profile.getAge(), profile.getPhone(), profile.getSkills(), profile.getPreferences(), profile.getTotalHours());
    }

    private Dtos.OrganizerResponse toOrganizerResponse(SysUser user) {
        OrganizerProfile profile = organizerProfileRepository.findById(user.getId()).orElse(null);
        String realName = profile != null ? profile.getRealName() : user.getUsername();
        String phone = profile != null ? profile.getPhone() : null;
        String department = profile != null ? profile.getDepartment() : null;
        long activityCount = activityRepository.countByOrganizerId(user.getId());
        return new Dtos.OrganizerResponse(user.getId(), user.getUsername(), user.getStatus(), realName, phone, department, activityCount);
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
