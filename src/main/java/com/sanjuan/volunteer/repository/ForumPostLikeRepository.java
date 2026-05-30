package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.ForumPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, ForumPostLike.ForumPostLikeId> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
