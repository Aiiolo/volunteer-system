package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Long> {
    boolean existsByActivityId(Long activityId);

    Optional<ActivitySummary> findByActivityId(Long activityId);

    List<ActivitySummary> findByOrganizerIdOrderBySubmitTimeDesc(Long organizerId);

    Optional<ActivitySummary> findByIdAndOrganizerId(Long id, Long organizerId);
}
