package com.kletsai.review;

import com.kletsai.TestcontainersConfiguration;
import com.kletsai.flashcard.Card;
import com.kletsai.flashcard.CardContent;
import com.kletsai.flashcard.CardRepository;
import com.kletsai.flashcard.CardService;
import com.kletsai.review.ReviewService.ReviewItem;
import com.kletsai.review.ReviewService.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ReviewServiceTests {

    @Autowired ReviewService reviews;
    @Autowired CardService cardService;
    @Autowired CardRepository cards;
    @Autowired ReviewLogRepository reviewLogs;

    private Card card;
    private final Instant t0 = Instant.parse("2026-10-01T07:00:00Z");

    @BeforeEach
    void createCard() {
        card = cardService.create(new CardContent(Card.CardType.WORD, "test-" + UUID.randomUUID(), "test",
                Card.Article.DE, null, null), Card.Source.MANUAL);
    }

    private ReviewItem review(Grade grade, Instant at) {
        return new ReviewItem(UUID.randomUUID(), card.getId(), grade.code(), at);
    }

    @Test
    void appliesReviewsInReviewedAtOrderNotRequestOrder() {
        ReviewItem first = review(Grade.GOOD, t0);
        ReviewItem second = review(Grade.GOOD, t0.plus(Duration.ofDays(1)));
        ReviewItem third = review(Grade.AGAIN, t0.plus(Duration.ofDays(7)));

        SyncResult result = reviews.sync(List.of(third, first, second));

        assertThat(result.applied()).containsExactly(first.id(), second.id(), third.id());
        Card updated = cards.findById(card.getId()).orElseThrow();
        // GOOD, GOOD → reps 2 / 6 days; then AGAIN resets to reps 0 / 1 day, EF 2.50 → 1.96
        assertThat(updated.getRepetitions()).isZero();
        assertThat(updated.getIntervalDays()).isEqualTo(1);
        assertThat(updated.getEaseFactor()).isEqualByComparingTo("1.96");
        assertThat(updated.getLastReviewedAt()).isEqualTo(third.reviewedAt());
    }

    @Test
    void resendingTheSameBatchIsHarmless() {
        ReviewItem r = review(Grade.GOOD, t0);

        reviews.sync(List.of(r));
        SyncResult again = reviews.sync(List.of(r, r));

        assertThat(again.applied()).isEmpty();
        assertThat(again.alreadySynced()).containsExactly(r.id());
        Card updated = cards.findById(card.getId()).orElseThrow();
        assertThat(updated.getRepetitions()).isEqualTo(1);
        assertThat(reviewLogs.findById(r.id())).isPresent();
    }

    @Test
    void duplicateIdsInsideOneBatchCountOnce() {
        ReviewItem r = review(Grade.GOOD, t0);

        SyncResult result = reviews.sync(List.of(r, r));

        assertThat(result.applied()).containsExactly(r.id());
        assertThat(cards.findById(card.getId()).orElseThrow().getRepetitions()).isEqualTo(1);
    }

    @Test
    void reviewsForDeletedCardsAreReportedNotFailed() {
        ReviewItem orphan = new ReviewItem(UUID.randomUUID(), UUID.randomUUID(), Grade.GOOD.code(), t0);

        SyncResult result = reviews.sync(List.of(orphan));

        assertThat(result.unknownCards()).containsExactly(orphan.id());
        assertThat(result.applied()).isEmpty();
    }

    @Test
    void futureTimestampsFromADriftingPhoneAreClampedToNow() {
        Instant before = Instant.now();
        ReviewItem r = review(Grade.GOOD, before.plus(Duration.ofDays(3)));

        reviews.sync(List.of(r));

        Card updated = cards.findById(card.getId()).orElseThrow();
        assertThat(updated.getLastReviewedAt()).isBetween(before, Instant.now());
    }
}
