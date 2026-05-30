package com.sanjuan.volunteer.service;

import com.sanjuan.volunteer.dto.Dtos;
import com.sanjuan.volunteer.entity.Activity;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.entity.SysUser;
import com.sanjuan.volunteer.exception.BusinessException;
import com.sanjuan.volunteer.repository.ActivityApplicationRepository;
import com.sanjuan.volunteer.repository.ActivityRepository;
import com.sanjuan.volunteer.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final SysUserRepository userRepository;
    private final ActivityApplicationRepository applicationRepository;

    public ActivityService(ActivityRepository activityRepository, SysUserRepository userRepository,
                           ActivityApplicationRepository applicationRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    public Page<Dtos.ActivityResponse> listPublished(Pageable pageable) {
        List<Dtos.ActivityResponse> registerable = listRegisterableActivities();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), registerable.size());
        List<Dtos.ActivityResponse> pageContent = start >= registerable.size()
                ? List.of() : registerable.subList(start, end);
        return new PageImpl<>(pageContent, pageable, registerable.size());
    }

    public List<Dtos.ActivityResponse> listLatestPublished(int limit) {
        return listRegisterableActivities().stream()
                .limit(Math.max(limit, 1))
                .toList();
    }

    private List<Dtos.ActivityResponse> listRegisterableActivities() {
        List<Enums.ActivityStatus> statuses = List.of(
                Enums.ActivityStatus.PUBLISHED,
                Enums.ActivityStatus.ONGOING,
                Enums.ActivityStatus.COMPLETED);
        return activityRepository.findByStatusIn(statuses).stream()
                .filter(activity -> {
                    Enums.ActivityStatus before = activity.getStatus();
                    ActivityStatusHelper.syncIfNeeded(activity);
                    if (activity.getStatus() != before) {
                        activityRepository.save(activity);
                    }
                    return ActivityStatusHelper.isOpenForRegistration(activity);
                })
                .sorted(Comparator.comparing(Activity::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public Page<Dtos.ActivityResponse> listAll(Pageable pageable) {
        return activityRepository.findAll(pageable).map(this::toResponseWithSync);
    }

    public List<Dtos.ActivityResponse> listByOrganizer(Long organizerId) {
        return activityRepository.findByOrganizerId(organizerId).stream()
                .map(this::toResponseWithSync)
                .toList();
    }

    @Transactional
    public Dtos.ActivityResponse create(Dtos.ActivityCreateRequest request) {
        SysUser organizer = userRepository.findById(request.organizerId())
                .orElseThrow(() -> new BusinessException("负责人不存在"));
        if (organizer.getRole() != Enums.Role.ORGANIZER) {
            throw new BusinessException("活动负责人角色不合法");
        }
        Activity activity = new Activity();
        applyFields(activity, request.title(), request.description(), request.requirements(), request.imageUrl(),
                request.maxVolunteers(), request.startTime(), request.endTime());
        activity.setOrganizer(organizer);
        activity.setStatus(Enums.ActivityStatus.DRAFT);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse update(Long id, Dtos.ActivityUpdateRequest request) {
        Activity activity = getActivity(id);
        applyUpdate(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse updateByAdmin(Long id, Dtos.ActivityUpdateRequest request) {
        Activity activity = getActivity(id);
        if (!isEditableByAdmin(activity.getStatus())) {
            throw new BusinessException("仅草稿或待审核活动可编辑");
        }
        if (request.status() != null) {
            Enums.ActivityStatus next = request.status();
            if (next == Enums.ActivityStatus.COMPLETED
                    && (activity.getStatus() == Enums.ActivityStatus.DRAFT
                    || activity.getStatus() == Enums.ActivityStatus.PENDING)) {
                throw new BusinessException("草稿或待审核活动不可直接设为已完结");
            }
            if (next == Enums.ActivityStatus.PUBLISHED || next == Enums.ActivityStatus.COMPLETED) {
                activity.setStatus(next);
                ActivityStatusHelper.syncIfNeeded(activity);
            } else if (next == Enums.ActivityStatus.DRAFT || next == Enums.ActivityStatus.PENDING) {
                activity.setStatus(next);
            } else {
                throw new BusinessException("无效的活动状态");
            }
            applyUpdate(activity, new Dtos.ActivityUpdateRequest(
                    request.title(), request.description(), request.requirements(), request.imageUrl(),
                    null, request.maxVolunteers(), request.startTime(), request.endTime()));
        } else {
            applyUpdate(activity, request);
        }
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse publishByAdmin(Long id) {
        Activity activity = getActivity(id);
        if (!isEditableByAdmin(activity.getStatus())) {
            throw new BusinessException("仅草稿或待审核活动可发布");
        }
        activity.setStatus(Enums.ActivityStatus.PUBLISHED);
        ActivityStatusHelper.syncIfNeeded(activity);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public void deleteByAdmin(Long id) {
        Activity activity = getActivity(id);
        if (!isEditableByAdmin(activity.getStatus())) {
            throw new BusinessException("仅草稿或待审核活动可删除");
        }
        activityRepository.delete(activity);
    }

    @Transactional
    public Dtos.ActivityResponse createByOrganizer(Long organizerId, Dtos.OrganizerActivitySaveRequest request, boolean submit) {
        SysUser organizer = requireOrganizer(organizerId);
        Activity activity = new Activity();
        applyOrganizerFields(activity, request);
        activity.setOrganizer(organizer);
        activity.setStatus(submit ? Enums.ActivityStatus.PENDING : Enums.ActivityStatus.DRAFT);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse updateByOrganizer(Long organizerId, Long activityId,
                                                   Dtos.OrganizerActivitySaveRequest request, boolean submit) {
        Activity activity = requireOwnedActivity(organizerId, activityId);
        if (!isEditableByOrganizer(activity.getStatus())) {
            throw new BusinessException("已发布活动不可编辑");
        }
        applyOrganizerFields(activity, request);
        if (submit) {
            activity.setStatus(Enums.ActivityStatus.PENDING);
        } else if (activity.getStatus() == Enums.ActivityStatus.DRAFT) {
            activity.setStatus(Enums.ActivityStatus.DRAFT);
        }
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse submitByOrganizer(Long organizerId, Long activityId) {
        Activity activity = requireOwnedActivity(organizerId, activityId);
        if (activity.getStatus() == Enums.ActivityStatus.PENDING) {
            throw new BusinessException("活动已在审核中");
        }
        if (activity.getStatus() != Enums.ActivityStatus.DRAFT) {
            throw new BusinessException("仅草稿活动可提交审核");
        }
        activity.setStatus(Enums.ActivityStatus.PENDING);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public Dtos.ActivityResponse withdrawByOrganizer(Long organizerId, Long activityId) {
        Activity activity = requireOwnedActivity(organizerId, activityId);
        if (activity.getStatus() != Enums.ActivityStatus.PENDING) {
            throw new BusinessException("仅待审核活动可取消审核");
        }
        activity.setStatus(Enums.ActivityStatus.DRAFT);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public void delete(Long id) {
        activityRepository.delete(getActivity(id));
    }

    @Transactional
    public void deleteDraftByOrganizer(Long organizerId, Long activityId) {
        Activity activity = requireOwnedActivity(organizerId, activityId);
        if (activity.getStatus() != Enums.ActivityStatus.DRAFT) {
            throw new BusinessException("仅草稿活动可以删除");
        }
        activityRepository.delete(activity);
    }

    public Activity getActivity(Long id) {
        return activityRepository.findById(id).orElseThrow(() -> new BusinessException("活动不存在"));
    }

    @Transactional
    public Dtos.ActivityResponse getActivitySynced(Long id) {
        Activity activity = getActivity(id);
        return toResponseWithSync(activity);
    }

    @Transactional
    public int syncAllPublishedLifecycleStatuses() {
        List<Enums.ActivityStatus> statuses = List.of(
                Enums.ActivityStatus.PUBLISHED,
                Enums.ActivityStatus.ONGOING,
                Enums.ActivityStatus.COMPLETED);
        int updated = 0;
        for (Activity activity : activityRepository.findByStatusIn(statuses)) {
            Enums.ActivityStatus before = activity.getStatus();
            ActivityStatusHelper.syncIfNeeded(activity);
            if (activity.getStatus() != before) {
                activityRepository.save(activity);
                updated++;
            }
        }
        return updated;
    }

    public void ensureOrganizerOwns(Long organizerId, Long activityId) {
        requireOwnedActivity(organizerId, activityId);
    }

    public Dtos.ActivityResponse toResponse(Activity activity) {
        long applicationCount = applicationRepository.countByActivityId(activity.getId());
        long approvedCount = applicationRepository.countQuotaOccupyingByActivityId(activity.getId());
        return new Dtos.ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getRequirements(),
                activity.getImageUrl(),
                activity.getStatus(),
                activity.getOrganizer().getId(),
                activity.getMaxVolunteers(),
                applicationCount,
                approvedCount,
                activity.getStartTime(),
                activity.getEndTime());
    }

    private Dtos.ActivityResponse toResponseWithSync(Activity activity) {
        Enums.ActivityStatus before = activity.getStatus();
        ActivityStatusHelper.syncIfNeeded(activity);
        if (activity.getStatus() != before) {
            activityRepository.save(activity);
        }
        return toResponse(activity);
    }

    private void applyFields(Activity activity, String title, String description, String requirements, String imageUrl,
                             Integer maxVolunteers, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("请上传活动图片");
        }
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setRequirements(requirements);
        activity.setImageUrl(imageUrl.trim());
        activity.setMaxVolunteers(maxVolunteers);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
    }

    private void applyOrganizerFields(Activity activity, Dtos.OrganizerActivitySaveRequest request) {
        applyFields(activity, request.title(), request.description(), request.requirements(), request.imageUrl(),
                request.maxVolunteers(), request.startTime(), request.endTime());
    }

    private void applyUpdate(Activity activity, Dtos.ActivityUpdateRequest request) {
        if (request.title() != null) activity.setTitle(request.title());
        if (request.description() != null) activity.setDescription(request.description());
        if (request.requirements() != null) activity.setRequirements(request.requirements());
        if (request.imageUrl() != null) {
            if (request.imageUrl().isBlank()) {
                throw new BusinessException("请上传活动图片");
            }
            activity.setImageUrl(request.imageUrl().trim());
        }
        if (request.status() != null) activity.setStatus(request.status());
        if (request.maxVolunteers() != null) {
            long occupied = applicationRepository.countQuotaOccupyingByActivityId(activity.getId());
            if (request.maxVolunteers() < occupied) {
                throw new BusinessException("名额上限不能小于当前已参与人数（" + occupied + "）");
            }
            activity.setMaxVolunteers(request.maxVolunteers());
        }
        if (request.startTime() != null) activity.setStartTime(request.startTime());
        if (request.endTime() != null) activity.setEndTime(request.endTime());
    }

    private boolean isEditableByAdmin(Enums.ActivityStatus status) {
        return status == Enums.ActivityStatus.DRAFT || status == Enums.ActivityStatus.PENDING;
    }

    private boolean isEditableByOrganizer(Enums.ActivityStatus status) {
        return status == Enums.ActivityStatus.DRAFT || status == Enums.ActivityStatus.PENDING;
    }

    private SysUser requireOrganizer(Long organizerId) {
        SysUser organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new BusinessException("负责人不存在"));
        if (organizer.getRole() != Enums.Role.ORGANIZER) {
            throw new BusinessException("活动负责人角色不合法");
        }
        return organizer;
    }

    private Activity requireOwnedActivity(Long organizerId, Long activityId) {
        Activity activity = getActivity(activityId);
        if (!activity.getOrganizer().getId().equals(organizerId)) {
            throw new BusinessException("只能操作本人负责的活动");
        }
        return activity;
    }
}
