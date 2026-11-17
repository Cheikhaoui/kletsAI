package com.kletsai.flashcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The editable part of a card: used for create and update requests. */
public record CardContent(
        @NotNull Card.CardType cardType,
        @NotBlank @Size(max = 500) String dutch,
        @NotBlank @Size(max = 500) String english,
        Card.Article article,
        @Size(max = 500) String exampleNl,
        @Size(max = 500) String exampleEn) {
}
