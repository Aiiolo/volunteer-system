package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "organizer_profile")
public class OrganizerProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private SysUser user;

    @Column(name = "real_name", nullable = false, length = 64)
    private String realName;

    @Column(length = 20)
    private String phone;

    @Column(length = 128)
    private String department;

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
