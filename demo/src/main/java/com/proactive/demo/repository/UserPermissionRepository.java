package com.proactive.demo.repository;

import com.proactive.demo.model.PermissionFeature;
import com.proactive.demo.model.User;
import com.proactive.demo.model.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    Optional<UserPermission> findByUserAndFeature(User user, PermissionFeature feature);

    List<UserPermission> findAllByUser(User user);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
