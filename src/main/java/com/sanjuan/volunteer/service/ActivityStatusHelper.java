package com.sanjuan.volunteer.service;

import com.sanjuan.volunteer.entity.Activity;
import com.sanjuan.volunteer.entity.Enums;

import java.time.LocalDateTime;

/**
 * 已发布活动的状态按时间自动流转：已发布 → 活动中 → 已完结
 */
public final class ActivityStatusHelper {
    private ActivityStatusHelper() {}

    public static boolean isPublishedLifecycle(Enums.ActivityStatus status) {
        return status == Enums.ActivityStatus.PUBLISHED
                || status == Enums.ActivityStatus.ONGOING
                || status == Enums.ActivityStatus.COMPLETED;
    }

    public static Enums.ActivityStatus resolve(Activity activity) {
        if (activity == null || !isPublishedLifecycle(activity.getStatus())) {
            return activity != null ? activity.getStatus() : null;
        }
        LocalDateTime start = activity.getStartTime();
        LocalDateTime end = activity.getEndTime();
        if (start == null || end == null) {
            return activity.getStatus();
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) {
            return Enums.ActivityStatus.PUBLISHED;
        }
        if (!now.isBefore(end)) {
            return Enums.ActivityStatus.COMPLETED;
        }
        return Enums.ActivityStatus.ONGOING;
    }

    public static boolean isTimeCompleted(Activity activity) {
        return resolve(activity) == Enums.ActivityStatus.COMPLETED;
    }

    public static boolean isOpenForRegistration(Activity activity) {
        return resolve(activity) == Enums.ActivityStatus.PUBLISHED;
    }

    public static void syncIfNeeded(Activity activity) {
        if (activity == null || !isPublishedLifecycle(activity.getStatus())) {
            return;
        }
        Enums.ActivityStatus next = resolve(activity);
        if (next != null && activity.getStatus() != next) {
            activity.setStatus(next);
        }
    }
}
