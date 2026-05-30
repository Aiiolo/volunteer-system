package com.sanjuan.volunteer.entity;

import java.util.EnumSet;
import java.util.Set;

public final class Enums {
    private Enums() {}

    public enum Role { VOLUNTEER, ADMIN, ORGANIZER }
    public enum UserStatus { PENDING, ACTIVE, DISABLED }
    public enum ActivityStatus { DRAFT, PENDING, PUBLISHED, ONGOING, COMPLETED }
    /** 报名/参与全流程状态 */
    public enum ApplicationStatus {
        PENDING,
        WAIT_CHECK_IN,
        WAIT_CHECK_IN_CONFIRM,
        WAIT_CHECK_OUT,
        WAIT_CHECK_OUT_CONFIRM,
        COMPLETED,
        REJECTED,
        ABSENT,
        /** @deprecated 兼容旧数据，审核通过时映射为 WAIT_CHECK_IN */
        APPROVED
    }
    public enum PostType { POST, NOTICE }

    private static final Set<ApplicationStatus> QUOTA_OCCUPYING = EnumSet.of(
            ApplicationStatus.WAIT_CHECK_IN,
            ApplicationStatus.WAIT_CHECK_IN_CONFIRM,
            ApplicationStatus.WAIT_CHECK_OUT,
            ApplicationStatus.WAIT_CHECK_OUT_CONFIRM,
            ApplicationStatus.COMPLETED,
            ApplicationStatus.APPROVED
    );

    public static boolean occupiesQuota(ApplicationStatus status) {
        return status != null && QUOTA_OCCUPYING.contains(status);
    }
}
