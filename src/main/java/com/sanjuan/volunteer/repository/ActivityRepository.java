package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.Activity;
import com.sanjuan.volunteer.entity.Enums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatus(Enums.ActivityStatus status, Pageable pageable);
    Page<Activity> findByStatusIn(List<Enums.ActivityStatus> statuses, Pageable pageable);
    List<Activity> findByStatusIn(List<Enums.ActivityStatus> statuses);
    List<Activity> findByOrganizerId(Long organizerId);
    long countByOrganizerId(Long organizerId);
    long countByStatusIn(List<Enums.ActivityStatus> statuses);
    List<Activity> findTop3ByStatusOrderByStartTimeDesc(Enums.ActivityStatus status);

    @org.springframework.data.jpa.repository.Query("""
            SELECT a FROM Activity a
            WHERE a.organizer.id = :organizerId
              AND a.endTime <= :now
              AND a.status IN (com.sanjuan.volunteer.entity.Enums.ActivityStatus.PUBLISHED,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.ONGOING,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.COMPLETED)
            ORDER BY a.endTime DESC
            """)
    List<Activity> findFinishedForSummaryByOrganizerId(Long organizerId, LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("""
            SELECT a FROM Activity a
            WHERE a.status IN (com.sanjuan.volunteer.entity.Enums.ActivityStatus.PUBLISHED,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.ONGOING,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.COMPLETED)
            ORDER BY a.startTime DESC
            """)
    List<Activity> findPublishedLifecycleActivities();

    @org.springframework.data.jpa.repository.Query("""
            SELECT a FROM Activity a
            WHERE a.organizer.id = :organizerId
              AND a.status IN (com.sanjuan.volunteer.entity.Enums.ActivityStatus.PUBLISHED,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.ONGOING,
                               com.sanjuan.volunteer.entity.Enums.ActivityStatus.COMPLETED)
            ORDER BY a.endTime DESC
            """)
    List<Activity> findPublishedLifecycleByOrganizerId(Long organizerId);
}
