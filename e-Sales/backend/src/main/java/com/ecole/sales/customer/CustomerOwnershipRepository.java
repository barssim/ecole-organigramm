package com.ecole.sales.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public interface CustomerOwnershipRepository extends JpaRepository<CustomerOwnership,Long> {
    Optional<CustomerOwnership> findByProductIdAndTenantId(String productId,String tenantId);
    List<CustomerOwnership> findAllByProductId(String productId);
    List<CustomerOwnership> findAllByProductIdAndOwnerId(String productId,Long ownerId);
    boolean existsByOwnerId(Long ownerId);
    @Transactional long deleteByProductIdAndTenantId(String productId,String tenantId);
}
