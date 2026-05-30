package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "activity_application", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "volunteer_id"}))
public class ActivityApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private SysUser volunteer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.ApplicationStatus status = Enums.ApplicationStatus.PENDING;

    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @PrePersist
    void prePersist() {
        applyTime = LocalDateTime.now();
    }
}
