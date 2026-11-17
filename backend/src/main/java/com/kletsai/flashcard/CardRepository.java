package com.kletsai.flashcard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Card> findByUserIdAndSourceOrderByCreatedAtDesc(UUID userId, Card.Source source);

    List<Card> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(UUID userId, Instant until);

    List<Card> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    Optional<Card> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndDueAtLessThanEqual(UUID userId, Instant until);

    List<Card> findByUserIdAndCardTypeAndDutchIgnoreCase(UUID userId, Card.CardType cardType, String dutch);
}
