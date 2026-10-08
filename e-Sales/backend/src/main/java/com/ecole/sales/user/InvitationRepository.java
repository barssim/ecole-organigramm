package com.ecole.sales.user;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface InvitationRepository extends JpaRepository<Invitation,Long> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitation i where i.tokenHash = :tokenHash")
    Optional<Invitation> lockByTokenHash(@Param("tokenHash") String tokenHash);
    @Modifying @Query("update Invitation i set i.usedAt = :now where i.user.id = :userId and i.usedAt is null")
    void invalidateForUser(@Param("userId") long userId,@Param("now") Instant now);
}
