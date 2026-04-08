package com.zipcode.stardust.repository;
import com.zipcode.stardust.model.UserProfile;

import com.zipcode.stardust.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {



    UserProfile findByUser(User user);
}