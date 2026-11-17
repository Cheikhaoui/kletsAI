package com.kletsai.review;

import com.kletsai.common.CurrentUser;
import com.kletsai.flashcard.Card;
import com.kletsai.flashcard.CardRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies reviews that were recorded on the phone (possibly offline).
 * SM-2 runs here only; the client never computes intervals.
 */
@Service
public class ReviewService {

    /** Device clocks drift; anything further in the future than this is clamped to "now". */
    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    public record ReviewItem(@NotNull UUID id, @NotNull UUID cardId,
                             @NotNull @Min(0) @Max(3) Integer grade, @NotNull Instant reviewedAt) {
    }

    /**
     * @param applied        review ids stored by this call
     * @param alreadySynced  review ids the server had already seen (safe to drop from the client queue)
     * @param unknownCards   review ids whose card no longer exists (deleted while offline); also safe to drop
     * @param cards          the latest state of every card touched
     */
    public record SyncResult(List<UUID> applied, List<UUID> alreadySynced, List<UUID> unknownCards,
                             List<Card> cards) {
    }

    private final CardRepository cards;
    private final ReviewLogRepository reviewLogs;
    private final Sm2Scheduler sm2;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ReviewService(CardRepository cards, ReviewLogRepository reviewLogs, Sm2Scheduler sm2,
                         CurrentUser currentUser, Clock clock) {
        this.cards = cards;
        this.reviewLogs = reviewLogs;
        this.sm2 = sm2;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional
    public SyncResult sync(List<ReviewItem> items) {
        UUID userId = currentUser.id();
        Instant now = clock.instant();

        // De-duplicate inside the batch (keep first), then drop ids already stored by an earlier sync.
        Map<UUID, ReviewItem> byId = new LinkedHashMap<>();
        items.forEach(i -> byId.putIfAbsent(i.id(), i));
        Set<UUID> existing = byId.isEmpty() ? Set.of() : reviewLogs.findExistingIds(byId.keySet());

        List<UUID> alreadySynced = new ArrayList<>(existing);
        List<ReviewItem> fresh = byId.values().stream()
                .filter(i -> !existing.contains(i.id()))
                .sorted(Comparator.comparing(ReviewItem::reviewedAt).thenComparing(ReviewItem::id))
                .toList();

        Map<UUID, Card> cardsById = fresh.isEmpty() ? Map.of() : cards
                .findByUserIdAndIdIn(userId, fresh.stream().map(ReviewItem::cardId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Card::getId, Function.identity()));

        List<UUID> applied = new ArrayList<>();
        List<UUID> unknownCards = new ArrayList<>();
        Map<UUID, Card> touched = new LinkedHashMap<>();

        for (ReviewItem item : fresh) {
            Card card = cardsById.get(item.cardId());
            if (card == null) {
                unknownCards.add(item.id());
                continue;
            }
            Instant reviewedAt = item.reviewedAt().isAfter(now.plus(MAX_CLOCK_SKEW)) ? now : item.reviewedAt();
            Grade grade = Grade.fromCode(item.grade());
            Sm2Scheduler.Result result = sm2.schedule(card.sm2State(), grade, reviewedAt);
            card.applyReview(result, reviewedAt);
            reviewLogs.save(new ReviewLog(item.id(), userId, card.getId(), grade, reviewedAt, now, result));
            applied.add(item.id());
            touched.put(card.getId(), card);
        }

        return new SyncResult(applied, alreadySynced, unknownCards, List.copyOf(touched.values()));
    }
}
