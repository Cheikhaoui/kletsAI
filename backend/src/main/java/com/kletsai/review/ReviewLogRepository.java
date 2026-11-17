package com.kletsai.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    @Query("select r.id from ReviewLog r where r.id in :ids")
    Set<UUID> findExistingIds(Collection<UUID> ids);

    long countByUserIdAndReviewedAtGreaterThanEqual(UUID userId, Instant since);
}
