package com.kletsai.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_log")
public class ReviewLog implements Persistable<UUID> {

    /** Generated on the client, so a retried sync never stores the same review twice. */
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(nullable = false)
    private short grade;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "interval_after", nullable = false)
    private int intervalAfter;

    @Column(name = "ease_after", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeAfter;

    protected ReviewLog() {
    }

    public ReviewLog(UUID id, UUID userId, UUID cardId, Grade grade, Instant reviewedAt, Instant syncedAt,
                     Sm2Scheduler.Result result) {
        this.id = id;
        this.userId = userId;
        this.cardId = cardId;
        this.grade = (short) grade.code();
        this.reviewedAt = reviewedAt;
        this.syncedAt = syncedAt;
        this.intervalAfter = result.intervalDays();
        this.easeAfter = result.easeFactor();
    }

    @Override
    public UUID getId() {
        return id;
    }

    /** Always new: we only ever insert reviews (avoids a SELECT before each INSERT). */
    @Override
    public boolean isNew() {
        return true;
    }

    public UUID getCardId() { return cardId; }
    public Grade getGrade() { return Grade.fromCode(grade); }
    public Instant getReviewedAt() { return reviewedAt; }
}
