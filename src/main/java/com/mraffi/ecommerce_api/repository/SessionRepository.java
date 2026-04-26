package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, String> {
   Optional<Session> findByUserIdAndDeviceHash(String userId, String deviceHash);
}
