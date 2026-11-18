package com.kletsai.ai;

import java.util.List;

/**
 * One tutor turn, as returned by Claude (structured output) after validation by {@link TutorReplyParser}.
 *
 * @param reply       what the tutor says: Dutch lines, each with its English translation
 * @param corrections mistakes in the learner's latest message (empty when it was correct)
 */
public record TutorReply(List<Line> reply, List<Correction> corrections) {

    public record Line(String nl, String en) {
    }

    public enum Category {
        WORD_ORDER, DE_HET, VERB_CONJUGATION, SPELLING, VOCABULARY, PREPOSITION, OTHER
    }

    /**
     * @param card optional flashcard suggestion; null means "make a sentence card from the correction"
     */
    public record Correction(String original, String corrected, String explanationEn, Category category,
                             CardSuggestion card) {
    }

    /** cardType is WORD or SENTENCE; article is DE, HET or null. */
    public record CardSuggestion(String cardType, String dutch, String english, String article) {
    }
}
