package com.kletsai.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Inject Clock instead of calling Instant.now() so tests can control time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
