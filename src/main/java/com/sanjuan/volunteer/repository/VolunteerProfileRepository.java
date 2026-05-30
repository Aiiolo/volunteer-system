package com.sanjuan.volunteer.repository;

import com.sanjuan.volunteer.entity.VolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long> {
}
