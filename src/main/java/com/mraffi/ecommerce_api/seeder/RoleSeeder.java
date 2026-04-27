package com.mraffi.ecommerce_api.seeder;

import com.mraffi.ecommerce_api.constant.PermissionConstant;
import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.entity.Permission;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.RolePermission;
import com.mraffi.ecommerce_api.repository.PermissionRepository;
import com.mraffi.ecommerce_api.repository.RolePermissionRepository;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleSeeder implements CommandLineRunner {

   private final RoleRepository roleRepository;
   private final PermissionRepository permissionRepository;
   private final RolePermissionRepository rolePermissionRepository;

   @Override
   @Transactional
   public void run(String... args) throws Exception {
      seedPermissions();
      seedRolesAndMapping();
      log.info("Authorization seeding process completed");
   }

   private void seedPermissions(){
      List<String> allPermissions = List.of(
              PermissionConstant.PRODUCT_READ,
              PermissionConstant.PRODUCT_CREATE,
              PermissionConstant.PRODUCT_UPDATE,
              PermissionConstant.PRODUCT_DELETE,
              PermissionConstant.USER_MANAGE,
              PermissionConstant.ROLE_PERMISSION_MANAGE
      );

      allPermissions.forEach(name -> {
         if(!permissionRepository.existsByName(name)){
            Permission p = Permission.create(name, "Access for " + name, name.split(":")[0]);
            permissionRepository.save(p);
            log.info("Permission {} created", name);
         }
      });
   }

   private void seedRolesAndMapping() {
      Arrays.stream(RoleName.values()).forEach(roleName -> {
         Role role = roleRepository.findByName(roleName.name())
                 .orElseGet(() -> roleRepository.save(Role.create(roleName.name())));

         Set<String> permissionsForRole = getPermissionsForRole(roleName);

         permissionsForRole.forEach(pName -> {
            Permission p = permissionRepository.findByName(pName).orElseThrow();

            if(!rolePermissionRepository.existsByRoleAndPermission(role, p)){
               RolePermission rp = RolePermission.create(role, p);
               rolePermissionRepository.save(rp);
            }
         });
      });
   }

   private Set<String> getPermissionsForRole(RoleName roleName){
      return switch (roleName) {
         case SUPER_ADMINISTRATOR -> Set.of(
               PermissionConstant.PRODUCT_READ,
               PermissionConstant.PRODUCT_CREATE,
               PermissionConstant.PRODUCT_UPDATE,
               PermissionConstant.PRODUCT_DELETE,
               PermissionConstant.USER_MANAGE,
               PermissionConstant.ROLE_PERMISSION_MANAGE
         );
         case ADMINISTRATOR -> Set.of(
                 PermissionConstant.PRODUCT_READ,
                 PermissionConstant.PRODUCT_CREATE,
                 PermissionConstant.PRODUCT_UPDATE,
                 PermissionConstant.PRODUCT_DELETE
         );
         case CUSTOMER -> Set.of(
                 PermissionConstant.PRODUCT_READ
         );
      };
   }
}
