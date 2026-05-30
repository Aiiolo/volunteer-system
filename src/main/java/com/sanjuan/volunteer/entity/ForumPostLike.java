package com.sanjuan.volunteer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "forum_post_like")
@IdClass(ForumPostLike.ForumPostLikeId.class)
public class ForumPostLike {
    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private SysUser user;

    @Getter
    @Setter
    public static class ForumPostLikeId implements Serializable {
        private Long postId;
        private Long userId;
    }
}
