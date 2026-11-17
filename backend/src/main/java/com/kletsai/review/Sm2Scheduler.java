package com.kletsai.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Classic SM-2 (SuperMemo 2), pure and stateless: no Spring, no database.
 *
 * <ul>
 *   <li>quality &lt; 3 (Again): repetitions reset to 0, interval 1 day</li>
 *   <li>otherwise: 1st success → 1 day, 2nd → 6 days, then round(interval × EF)</li>
 *   <li>EF' = EF + (0.1 − (5−q) × (0.08 + (5−q) × 0.02)), never below 1.3, rounded to 2 decimals</li>
 * </ul>
 * Deltas per button: Again −0.54, Hard −0.14, Good ±0, Easy +0.10.
 */
public final class Sm2Scheduler {

    public static final BigDecimal MIN_EASE = new BigDecimal("1.30");
    public static final BigDecimal INITIAL_EASE = new BigDecimal("2.50");

    public record State(BigDecimal easeFactor, int intervalDays, int repetitions) {
        public static State initial() {
            return new State(INITIAL_EASE, 0, 0);
        }
    }

    public record Result(BigDecimal easeFactor, int intervalDays, int repetitions, Instant dueAt) {
        public State state() {
            return new State(easeFactor, intervalDays, repetitions);
        }
    }

    public Result schedule(State current, Grade grade, Instant reviewedAt) {
        int q = grade.quality();

        int repetitions;
        int interval;
        if (q < 3) {
            repetitions = 0;
            interval = 1;
        } else {
            repetitions = current.repetitions() + 1;
            interval = switch (repetitions) {
                case 1 -> 1;
                case 2 -> 6;
                default -> (int) Math.round(Math.max(current.intervalDays(), 1) * current.easeFactor().doubleValue());
            };
        }

        double delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02);
        BigDecimal ease = current.easeFactor()
                .add(BigDecimal.valueOf(delta))
                .setScale(2, RoundingMode.HALF_UP)
                .max(MIN_EASE);

        return new Result(ease, interval, repetitions, reviewedAt.plus(Duration.ofDays(interval)));
    }
}
