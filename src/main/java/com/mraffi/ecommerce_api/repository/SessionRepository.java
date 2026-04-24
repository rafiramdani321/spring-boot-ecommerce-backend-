package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {

}
