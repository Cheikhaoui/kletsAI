package com.kletsai.flashcard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        Card.CardType cardType,
        String dutch,
        String english,
        Card.Article article,
        String exampleNl,
        String exampleEn,
        Card.Source source,
        UUID mistakeId,
        BigDecimal easeFactor,
        int intervalDays,
        int repetitions,
        Instant dueAt,
        Instant lastReviewedAt) {

    public static CardResponse from(Card c) {
        return new CardResponse(c.getId(), c.getCardType(), c.getDutch(), c.getEnglish(), c.getArticle(),
                c.getExampleNl(), c.getExampleEn(), c.getSource(), c.getMistakeId(), c.getEaseFactor(),
                c.getIntervalDays(), c.getRepetitions(), c.getDueAt(), c.getLastReviewedAt());
    }
}
