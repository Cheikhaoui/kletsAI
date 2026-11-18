package com.kletsai.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AiConfig {

    @Bean
    TutorReplyParser tutorReplyParser(JsonMapper jsonMapper) {
        return new TutorReplyParser(jsonMapper);
    }
}
