package com.kletsai.ai;

import com.kletsai.ai.TutorReply.CardSuggestion;
import com.kletsai.ai.TutorReply.Category;
import com.kletsai.ai.TutorReply.Correction;
import com.kletsai.ai.TutorReply.Line;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns Claude's JSON into a clean {@link TutorReply}. Pure and deterministic, so it is unit tested
 * with fixed JSON. Structured outputs guarantee the shape; this class guards the content:
 * blank lines, corrections that change nothing, duplicates, unknown categories and over-long text.
 */
public class TutorReplyParser {

    static final int MAX_TEXT = 500;
    static final int MAX_LINES = 8;
    static final int MAX_CORRECTIONS = 5;

    private final JsonMapper json;

    public TutorReplyParser(JsonMapper json) {
        this.json = json;
    }

    public TutorReply parse(String raw) {
        JsonNode root;
        try {
            root = json.readTree(raw);
        } catch (JacksonException e) {
            throw new TutorUnavailableException("Tutor reply was not valid JSON", e);
        }
        if (root == null || !root.isObject()) {
            throw new TutorUnavailableException("Tutor reply was not a JSON object");
        }

        List<Line> lines = new ArrayList<>();
        for (JsonNode n : root.path("reply")) {
            String nl = text(n, "nl");
            if (nl != null && lines.size() < MAX_LINES) {
                lines.add(new Line(nl, orEmpty(text(n, "en"))));
            }
        }
        if (lines.isEmpty()) {
            throw new TutorUnavailableException("Tutor reply had no Dutch lines");
        }

        List<Correction> corrections = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode n : root.path("corrections")) {
            String original = text(n, "original");
            String corrected = text(n, "corrected");
            if (original == null || corrected == null) {
                continue;
            }
            String key = normalize(original) + "→" + normalize(corrected);
            if (normalize(original).equals(normalize(corrected)) || !seen.add(key)) {
                continue; // a "correction" that only fixes capitals/punctuation, or a duplicate
            }
            corrections.add(new Correction(original, corrected, text(n, "explanationEn"),
                    category(text(n, "category")), card(n.path("card"))));
            if (corrections.size() == MAX_CORRECTIONS) {
                break;
            }
        }
        return new TutorReply(List.copyOf(lines), List.copyOf(corrections));
    }

    private CardSuggestion card(JsonNode n) {
        if (!n.isObject()) {
            return null;
        }
        String dutch = text(n, "dutch");
        String english = text(n, "english");
        if (dutch == null || english == null) {
            return null;
        }
        String type = "SENTENCE".equalsIgnoreCase(text(n, "cardType")) ? "SENTENCE" : "WORD";
        String article = text(n, "article");
        article = article == null ? null : switch (article.toUpperCase(Locale.ROOT)) {
            case "DE", "HET" -> article.toUpperCase(Locale.ROOT);
            default -> null;
        };
        if (type.equals("WORD") && article != null) {
            dutch = stripArticle(dutch);
        }
        return new CardSuggestion(type, dutch, english, type.equals("WORD") ? article : null);
    }

    private static Category category(String s) {
        if (s == null) {
            return Category.OTHER;
        }
        try {
            return Category.valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return Category.OTHER;
        }
    }

    /** "de trein" → "trein": the article is stored separately. */
    static String stripArticle(String dutch) {
        return dutch.replaceFirst("(?i)^(de|het)\\s+", "");
    }

    /** Lower case, no accents, no punctuation, single spaces: "Ik  ga, naar huis." == "ik ga naar huis". */
    static String normalize(String s) {
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll("\\s+", " ").trim();
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isString()) {
            return null;
        }
        String s = v.asString().strip();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) : s;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
