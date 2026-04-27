package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
   Boolean existsByName(String name);
   Optional<Permission> findByName(String name);
}
