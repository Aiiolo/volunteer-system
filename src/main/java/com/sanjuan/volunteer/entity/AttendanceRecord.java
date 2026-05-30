package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "attendance_record", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "volunteer_id"}))
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private SysUser volunteer;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_requested", nullable = false)
    private Boolean checkInRequested = false;

    @Column(name = "check_out_requested", nullable = false)
    private Boolean checkOutRequested = false;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal hours = BigDecimal.ZERO;

    @Column(length = 1024)
    private String evaluation;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = createTime;
    }

    @PreUpdate
    void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
