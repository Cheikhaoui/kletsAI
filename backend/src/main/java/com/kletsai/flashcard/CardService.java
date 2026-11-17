package com.kletsai.flashcard;

import com.kletsai.common.CurrentUser;
import com.kletsai.common.NotFoundException;
import com.kletsai.review.ReviewLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    public static final int MAX_DECK_DAYS = 7;

    public record CardStats(long dueNow, long total, long reviewedToday) {
    }

    private final CardRepository cards;
    private final ReviewLogRepository reviewLogs;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final ZoneId zone;

    public CardService(CardRepository cards, ReviewLogRepository reviewLogs, CurrentUser currentUser, Clock clock,
                       @Value("${kletsai.time-zone}") ZoneId zone) {
        this.cards = cards;
        this.reviewLogs = reviewLogs;
        this.currentUser = currentUser;
        this.clock = clock;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public List<Card> list(Card.Source source) {
        UUID userId = currentUser.id();
        return source == null
                ? cards.findByUserIdOrderByCreatedAtDesc(userId)
                : cards.findByUserIdAndSourceOrderByCreatedAtDesc(userId, source);
    }

    @Transactional(readOnly = true)
    public Card get(UUID id) {
        return cards.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Card", id));
    }

    /** Cards due now and in the next {@code days} days, so the phone has enough cards for an offline ride. */
    @Transactional(readOnly = true)
    public List<Card> reviewDeck(int days) {
        int clamped = Math.clamp(days, 0, MAX_DECK_DAYS);
        Instant until = clock.instant().plus(Duration.ofDays(clamped));
        return cards.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(currentUser.id(), until);
    }

    @Transactional(readOnly = true)
    public CardStats stats() {
        UUID userId = currentUser.id();
        Instant now = clock.instant();
        Instant startOfDay = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        return new CardStats(
                cards.countByUserIdAndDueAtLessThanEqual(userId, now),
                cards.countByUserId(userId),
                reviewLogs.countByUserIdAndReviewedAtGreaterThanEqual(userId, startOfDay));
    }

    @Transactional
    public Card create(CardContent content, Card.Source source) {
        UUID userId = currentUser.id();
        ensureNotDuplicate(userId, content, null);
        return cards.save(Card.create(userId, source, content));
    }

    @Transactional
    public Card update(UUID id, CardContent content) {
        Card card = get(id);
        ensureNotDuplicate(card.getUserId(), content, id);
        card.applyContent(content);
        return card;
    }

    @Transactional
    public void delete(UUID id) {
        cards.delete(get(id));
    }

    private void ensureNotDuplicate(UUID userId, CardContent content, UUID excludeId) {
        boolean duplicate = cards.findByUserIdAndCardTypeAndDutchIgnoreCase(userId, content.cardType(), content.dutch().strip())
                .stream().anyMatch(c -> !c.getId().equals(excludeId));
        if (duplicate) {
            throw new DuplicateCardException(content.dutch().strip());
        }
    }
}
