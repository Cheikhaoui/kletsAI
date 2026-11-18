package com.kletsai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param model     Claude model id
 * @param maxTokens output cap per reply (includes thinking)
 * @param effort    low | medium | high: short chat turns do well on low
 * @param maxHistoryMessages how many earlier messages are sent with each turn
 */
@ConfigurationProperties("kletsai.ai")
public record AiProperties(String model, int maxTokens, String effort, int maxHistoryMessages) {
}
