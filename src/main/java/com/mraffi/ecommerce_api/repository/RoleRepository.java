package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

   Optional<Role> findByName(String name);
   boolean existsByName(String name);

}
