package com.kletsai.review;

import com.kletsai.review.Sm2Scheduler.Result;
import com.kletsai.review.Sm2Scheduler.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2SchedulerTests {

    private final Sm2Scheduler sm2 = new Sm2Scheduler();
    private final Instant now = Instant.parse("2026-11-18T07:30:00Z");

    private static State state(String ef, int interval, int reps) {
        return new State(new BigDecimal(ef), interval, reps);
    }

    @ParameterizedTest(name = "{0} on a new card → EF {1}")
    @CsvSource({
            "AGAIN, 1.96",
            "HARD,  2.36",
            "GOOD,  2.50",
            "EASY,  2.60",
    })
    void easeChangesPerGrade(Grade grade, String expectedEase) {
        Result r = sm2.schedule(State.initial(), grade, now);
        assertThat(r.easeFactor()).isEqualByComparingTo(expectedEase);
    }

    @Test
    void successfulReviewsFollowOneSixThenTimesEase() {
        Result first = sm2.schedule(State.initial(), Grade.GOOD, now);
        assertThat(first.intervalDays()).isEqualTo(1);
        assertThat(first.repetitions()).isEqualTo(1);

        Result second = sm2.schedule(first.state(), Grade.GOOD, now);
        assertThat(second.intervalDays()).isEqualTo(6);
        assertThat(second.repetitions()).isEqualTo(2);

        Result third = sm2.schedule(second.state(), Grade.GOOD, now);
        assertThat(third.intervalDays()).isEqualTo(15); // round(6 × 2.5)
        assertThat(third.repetitions()).isEqualTo(3);

        Result fourth = sm2.schedule(third.state(), Grade.GOOD, now);
        assertThat(fourth.intervalDays()).isEqualTo(38); // round(15 × 2.5) = 37.5 → 38
    }

    @Test
    void againResetsRepetitionsAndIntervalButKeepsLearningHistoryInEase() {
        Result r = sm2.schedule(state("2.50", 38, 4), Grade.AGAIN, now);
        assertThat(r.repetitions()).isZero();
        assertThat(r.intervalDays()).isEqualTo(1);
        assertThat(r.easeFactor()).isEqualByComparingTo("1.96");
    }

    @Test
    void hardStillCountsAsSuccessButLowersEase() {
        Result r = sm2.schedule(state("2.50", 6, 2), Grade.HARD, now);
        assertThat(r.repetitions()).isEqualTo(3);
        assertThat(r.intervalDays()).isEqualTo(15); // uses the EF from before this review
        assertThat(r.easeFactor()).isEqualByComparingTo("2.36");
    }

    @Test
    void easeNeverDropsBelowFloor() {
        Result r = sm2.schedule(state("1.40", 1, 0), Grade.AGAIN, now);
        assertThat(r.easeFactor()).isEqualByComparingTo(Sm2Scheduler.MIN_EASE);

        Result again = sm2.schedule(r.state(), Grade.AGAIN, now);
        assertThat(again.easeFactor()).isEqualByComparingTo("1.30");
    }

    @Test
    void dueAtIsReviewTimePlusInterval() {
        Result r = sm2.schedule(state("2.50", 6, 2), Grade.EASY, now);
        assertThat(r.dueAt()).isEqualTo(now.plus(Duration.ofDays(15)));
    }

    @Test
    void gradeCodesRoundTrip() {
        for (Grade g : Grade.values()) {
            assertThat(Grade.fromCode(g.code())).isEqualTo(g);
        }
    }
}
