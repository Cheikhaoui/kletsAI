package com.kletsai.review;

/** The four answer buttons. The code is what the client sends and what review_log stores. */
public enum Grade {
    AGAIN(0, 1),
    HARD(1, 3),
    GOOD(2, 4),
    EASY(3, 5);

    private final int code;
    /** SM-2 quality (0..5). Below 3 counts as a failed recall. */
    private final int quality;

    Grade(int code, int quality) {
        this.code = code;
        this.quality = quality;
    }

    public int code() {
        return code;
    }

    public int quality() {
        return quality;
    }

    public static Grade fromCode(int code) {
        for (Grade g : values()) {
            if (g.code == code) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown grade code: " + code);
    }
}
