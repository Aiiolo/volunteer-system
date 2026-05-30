package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByActivityIdAndVolunteerId(Long activityId, Long volunteerId);
    List<AttendanceRecord> findByVolunteerId(Long volunteerId);

    @Query("select coalesce(sum(a.hours), 0) from AttendanceRecord a")
    BigDecimal sumAllHours();

    @Query("SELECT DISTINCT a.activity.id FROM AttendanceRecord a WHERE a.volunteer.id = :volunteerId AND a.checkOutTime IS NOT NULL")
    List<Long> findCompletedActivityIdsByVolunteerId(Long volunteerId);

    boolean existsByVolunteerIdAndActivityIdAndCheckOutTimeIsNotNull(Long volunteerId, Long activityId);

    boolean existsByVolunteerIdAndActivityIdAndCheckInTimeIsNotNull(Long volunteerId, Long activityId);

    @Query("""
            select case when count(ar) > 0 then true else false end from AttendanceRecord ar
            where ar.volunteer.id = :volunteerId and ar.activity.id = :activityId
              and (ar.checkInTime is not null or ar.checkOutTime is not null)
            """)
    boolean existsParticipated(@Param("volunteerId") Long volunteerId, @Param("activityId") Long activityId);

    @Query("""
            select coalesce(sum(ar.hours), 0) from AttendanceRecord ar
            join ar.activity a
            where a.organizer.id = :organizerId and ar.checkOutTime is not null
            """)
    BigDecimal sumCheckedOutHoursByOrganizerId(@Param("organizerId") Long organizerId);
}
