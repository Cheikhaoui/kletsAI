package com.kletsai.flashcard;

public class DuplicateCardException extends RuntimeException {

    public DuplicateCardException(String dutch) {
        super("You already have a card for \"" + dutch + "\"");
    }
}
