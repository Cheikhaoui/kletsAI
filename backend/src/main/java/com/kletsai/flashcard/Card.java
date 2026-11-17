package com.kletsai.flashcard;

import com.kletsai.review.Sm2Scheduler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card")
public class Card {

    public enum CardType { WORD, SENTENCE }

    public enum Article { DE, HET }

    public enum Source { MANUAL, SEED, MISTAKE }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(nullable = false)
    private String dutch;

    @Column(nullable = false)
    private String english;

    @Enumerated(EnumType.STRING)
    private Article article;

    @Column(name = "example_nl")
    private String exampleNl;

    @Column(name = "example_en")
    private String exampleEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Source source;

    @Column(name = "mistake_id")
    private UUID mistakeId;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private int repetitions;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Card() {
    }

    public static Card create(UUID userId, Source source, CardContent content) {
        Card card = new Card();
        card.id = UUID.randomUUID();
        card.userId = userId;
        card.source = source;
        card.applyContent(content);
        Sm2Scheduler.State initial = Sm2Scheduler.State.initial();
        card.easeFactor = initial.easeFactor();
        card.intervalDays = initial.intervalDays();
        card.repetitions = initial.repetitions();
        card.dueAt = Instant.now();
        return card;
    }

    public void applyContent(CardContent content) {
        this.cardType = content.cardType();
        this.dutch = content.dutch().strip();
        this.english = content.english().strip();
        this.article = content.cardType() == CardType.WORD ? content.article() : null;
        this.exampleNl = blankToNull(content.exampleNl());
        this.exampleEn = blankToNull(content.exampleEn());
    }

    public void linkMistake(UUID mistakeId) {
        this.mistakeId = mistakeId;
    }

    public Sm2Scheduler.State sm2State() {
        return new Sm2Scheduler.State(easeFactor, intervalDays, repetitions);
    }

    public void applyReview(Sm2Scheduler.Result result, Instant reviewedAt) {
        this.easeFactor = result.easeFactor();
        this.intervalDays = result.intervalDays();
        this.repetitions = result.repetitions();
        this.dueAt = result.dueAt();
        this.lastReviewedAt = reviewedAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public CardType getCardType() { return cardType; }
    public String getDutch() { return dutch; }
    public String getEnglish() { return english; }
    public Article getArticle() { return article; }
    public String getExampleNl() { return exampleNl; }
    public String getExampleEn() { return exampleEn; }
    public Source getSource() { return source; }
    public UUID getMistakeId() { return mistakeId; }
    public BigDecimal getEaseFactor() { return easeFactor; }
    public int getIntervalDays() { return intervalDays; }
    public int getRepetitions() { return repetitions; }
    public Instant getDueAt() { return dueAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
