package com.kletsai.review;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewConfig {

    /** Sm2Scheduler stays a plain class (no Spring annotations) so it is trivial to unit test. */
    @Bean
    Sm2Scheduler sm2Scheduler() {
        return new Sm2Scheduler();
    }
}
