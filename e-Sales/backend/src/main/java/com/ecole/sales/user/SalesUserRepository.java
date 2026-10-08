package com.ecole.sales.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface SalesUserRepository extends JpaRepository<SalesUser,Long> {
    Optional<SalesUser> findByUsername(String username);
    List<SalesUser> findAllByRoleOrderByUsername(String role);
    boolean existsByUsername(String username);
    long countByRole(String role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from SalesUser u where u.id = :id")
    Optional<SalesUser> lockById(@Param("id") long id);
}
