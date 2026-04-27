package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Permission;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {

   Boolean existsByRoleAndPermission(Role role, Permission permission);

}
