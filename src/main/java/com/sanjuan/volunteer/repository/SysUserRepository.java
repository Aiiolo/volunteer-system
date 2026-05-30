package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    boolean existsByUsername(String username);
    Optional<SysUser> findByUsername(String username);
    List<SysUser> findByRole(Enums.Role role);
}
