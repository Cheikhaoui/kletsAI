package com.kletsai.ai;

/** Claude could not produce a usable reply (API error, refusal, cut-off or invalid JSON). */
public class TutorUnavailableException extends RuntimeException {

    public TutorUnavailableException(String message) {
        super(message);
    }

    public TutorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
