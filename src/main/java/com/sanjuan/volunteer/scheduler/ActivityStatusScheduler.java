package com.sanjuan.volunteer.scheduler;

import com.sanjuan.volunteer.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时同步已发布活动的状态（报名中 → 进行中 → 已结束），避免长期无人访问时状态滞后。
 */
@Component
public class ActivityStatusScheduler {
    private static final Logger log = LoggerFactory.getLogger(ActivityStatusScheduler.class);

    private final ActivityService activityService;

    public ActivityStatusScheduler(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 10_000)
    public void syncPublishedLifecycleStatuses() {
        try {
            int updated = activityService.syncAllPublishedLifecycleStatuses();
            if (updated > 0) {
                log.debug("活动状态定时同步：更新 {} 条", updated);
            }
        } catch (Exception ex) {
            log.warn("活动状态定时同步失败", ex);
        }
    }
}
