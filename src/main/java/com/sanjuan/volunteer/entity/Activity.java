package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "activity")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 1024)
    private String requirements;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.ActivityStatus status = Enums.ActivityStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private SysUser organizer;

    @Column(name = "max_volunteers", nullable = false)
    private Integer maxVolunteers;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

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
