package com.mraffi.ecommerce_api.seeder;

import com.mraffi.ecommerce_api.contant.RoleName;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

   private final RoleRepository roleRepository;

   @Override
   public void run(String... args) throws Exception {
      seedRole(RoleName.CUSTOMER.name());
      seedRole(RoleName.ADMINISTRATOR.name());
   }

   private void seedRole(String roleName){
      if(!roleRepository.existsByName(roleName)){
         Role role = Role.create(roleName);
         roleRepository.save(role);

      }
   }
}
