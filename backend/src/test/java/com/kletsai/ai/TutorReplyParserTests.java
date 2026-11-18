package com.kletsai.ai;

import com.kletsai.ai.TutorReply.Category;
import com.kletsai.ai.TutorReply.Correction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TutorReplyParserTests {

    private final TutorReplyParser parser = new TutorReplyParser(JsonMapper.builder().build());

    @Test
    void parsesLinesAndCorrections() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Goedemorgen! Waar wil je naartoe?","en":"Good morning! Where do you want to go?"}],
                 "corrections":[{"original":"Ik wil gaan naar Amsterdam","corrected":"Ik wil naar Amsterdam gaan",
                   "explanationEn":"The infinitive goes to the end.","category":"WORD_ORDER",
                   "card":{"cardType":"SENTENCE","dutch":"Ik wil naar Amsterdam gaan.","english":"I want to go to Amsterdam.","article":null}}]}
                """);

        assertThat(r.reply()).singleElement().satisfies(l -> {
            assertThat(l.nl()).isEqualTo("Goedemorgen! Waar wil je naartoe?");
            assertThat(l.en()).isEqualTo("Good morning! Where do you want to go?");
        });
        Correction c = r.corrections().getFirst();
        assertThat(c.category()).isEqualTo(Category.WORD_ORDER);
        assertThat(c.corrected()).isEqualTo("Ik wil naar Amsterdam gaan");
        assertThat(c.card().cardType()).isEqualTo("SENTENCE");
        assertThat(c.card().article()).isNull();
    }

    @Test
    void emptyCorrectionsWhenMessageWasRight() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Heel goed!","en":"Very good!"}],"corrections":[]}
                """);
        assertThat(r.corrections()).isEmpty();
    }

    @Test
    void dropsCorrectionsThatOnlyChangeCapitalsOrPunctuation() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Ja.","en":"Yes."}],
                 "corrections":[{"original":"ik woon in utrecht","corrected":"Ik woon in Utrecht.","explanationEn":"x","category":"SPELLING"}]}
                """);
        assertThat(r.corrections()).isEmpty();
    }

    @Test
    void dropsDuplicateAndIncompleteCorrections() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Ja.","en":"Yes."}],
                 "corrections":[
                   {"original":"het trein","corrected":"de trein","explanationEn":"Trein is a de-word.","category":"DE_HET"},
                   {"original":"Het trein","corrected":"De trein.","explanationEn":"again","category":"DE_HET"},
                   {"original":"","corrected":"de fiets","explanationEn":"x","category":"DE_HET"},
                   {"original":"ik gaat","explanationEn":"missing corrected","category":"VERB_CONJUGATION"}]}
                """);
        assertThat(r.corrections()).singleElement()
                .satisfies(c -> assertThat(c.original()).isEqualTo("het trein"));
    }

    @Test
    void unknownCategoryBecomesOther() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Ja.","en":"Yes."}],
                 "corrections":[{"original":"ik ben 30 jaren","corrected":"ik ben 30 jaar","explanationEn":"x","category":"PLURALS"}]}
                """);
        assertThat(r.corrections().getFirst().category()).isEqualTo(Category.OTHER);
    }

    @Test
    void wordCardLosesItsArticleAndKeepsItSeparately() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"Ja.","en":"Yes."}],
                 "corrections":[{"original":"het trein","corrected":"de trein","explanationEn":"x","category":"DE_HET",
                   "card":{"cardType":"WORD","dutch":"de trein","english":"train","article":"de"}}]}
                """);
        TutorReply.CardSuggestion card = r.corrections().getFirst().card();
        assertThat(card.dutch()).isEqualTo("trein");
        assertThat(card.article()).isEqualTo("DE");
    }

    @Test
    void skipsBlankLinesButFailsWhenNoDutchIsLeft() {
        TutorReply r = parser.parse("""
                {"reply":[{"nl":"  ","en":"blank"},{"nl":"Hallo!","en":"Hello!"}],"corrections":[]}
                """);
        assertThat(r.reply()).extracting(TutorReply.Line::nl).containsExactly("Hallo!");

        assertThatThrownBy(() -> parser.parse("{\"reply\":[],\"corrections\":[]}"))
                .isInstanceOf(TutorUnavailableException.class);
    }

    @Test
    void invalidJsonIsATutorError() {
        assertThatThrownBy(() -> parser.parse("Sorry, I can't help with that."))
                .isInstanceOf(TutorUnavailableException.class);
    }

    @Test
    void normalizeIgnoresCaseAccentsPunctuationAndSpaces() {
        assertThat(TutorReplyParser.normalize("  Één  kopje, koffie! ")).isEqualTo("een kopje koffie");
    }
}
