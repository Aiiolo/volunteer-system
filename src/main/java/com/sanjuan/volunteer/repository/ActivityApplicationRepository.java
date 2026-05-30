package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.ActivityApplication;
import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.service.ApplicationStatusHelper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityApplicationRepository extends JpaRepository<ActivityApplication, Long> {
    boolean existsByActivityIdAndVolunteerId(Long activityId, Long volunteerId);
    long countByActivityId(Long activityId);
    long countByStatus(Enums.ApplicationStatus status);
    List<ActivityApplication> findByVolunteerId(Long volunteerId);
    List<ActivityApplication> findByActivityId(Long activityId);
    Optional<ActivityApplication> findByActivityIdAndVolunteerId(Long activityId, Long volunteerId);

    @Query("select count(aa) from ActivityApplication aa where aa.activity.id = :activityId and aa.status in :statuses")
    long countByActivityIdAndStatusIn(@Param("activityId") Long activityId,
                                      @Param("statuses") Collection<Enums.ApplicationStatus> statuses);

    default long countQuotaOccupyingByActivityId(Long activityId) {
        return countByActivityIdAndStatusIn(activityId, ApplicationStatusHelper.QUOTA_STATUSES);
    }

    @Query("""
            select count(distinct aa.volunteer.id) from ActivityApplication aa
            join aa.activity a
            where a.organizer.id = :organizerId and aa.status in :statuses
            """)
    long countDistinctParticipatingVolunteersByOrganizerId(@Param("organizerId") Long organizerId,
                                                           @Param("statuses") Collection<Enums.ApplicationStatus> statuses);

    default long countDistinctParticipatingVolunteersByOrganizerId(Long organizerId) {
        return countDistinctParticipatingVolunteersByOrganizerId(organizerId, ApplicationStatusHelper.QUOTA_STATUSES);
    }

    @Query("""
            select count(aa) from ActivityApplication aa
            join aa.activity a
            where a.organizer.id = :organizerId
              and aa.status in :waitStatuses
            """)
    long countPendingCheckInByOrganizerId(@Param("organizerId") Long organizerId,
                                          @Param("waitStatuses") Collection<Enums.ApplicationStatus> waitStatuses);

    default long countPendingCheckInByOrganizerId(Long organizerId) {
        return countPendingCheckInByOrganizerId(organizerId,
                List.of(Enums.ApplicationStatus.WAIT_CHECK_IN, Enums.ApplicationStatus.WAIT_CHECK_IN_CONFIRM));
    }

    @Query("""
            select distinct aa.activity from ActivityApplication aa
            where aa.volunteer.id = :volunteerId
              and aa.status in :statuses
            order by aa.activity.startTime desc
            """)
    List<com.sanjuan.volunteer.entity.Activity> findVolunteerJoinedActivities(@Param("volunteerId") Long volunteerId,
                                                                              @Param("statuses") Collection<Enums.ApplicationStatus> statuses);

    default List<com.sanjuan.volunteer.entity.Activity> findVolunteerJoinedActivities(Long volunteerId) {
        return findVolunteerJoinedActivities(volunteerId, ApplicationStatusHelper.QUOTA_STATUSES);
    }

    boolean existsByVolunteerIdAndActivityIdAndStatus(Long volunteerId, Long activityId, Enums.ApplicationStatus status);

    @Query("""
            select case when count(aa) > 0 then true else false end from ActivityApplication aa
            where aa.volunteer.id = :volunteerId and aa.activity.id = :activityId
              and aa.status <> com.sanjuan.volunteer.entity.Enums.ApplicationStatus.REJECTED
            """)
    boolean existsJoinedApplication(@Param("volunteerId") Long volunteerId, @Param("activityId") Long activityId);
}
