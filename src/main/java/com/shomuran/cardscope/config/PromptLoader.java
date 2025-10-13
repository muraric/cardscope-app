package com.shomuran.cardscope.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {
    private final String cardSuggestionPrompt;
    private final String cardRewardPrompt;
    private final String cardSuggestionJsonPrompt;


    public PromptLoader(ResourceLoader resourceLoader) throws IOException {
        Resource resource1 = resourceLoader.getResource("classpath:prompts/card-suggestion.txt");
        this.cardSuggestionPrompt = new String(resource1.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Resource resource2 = resourceLoader.getResource("classpath:prompts/card-rewards.txt");
        this.cardRewardPrompt = new String(resource2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Resource resource3 = resourceLoader.getResource("classpath:prompts/card-suggestion-with-json.txt");
        this.cardSuggestionJsonPrompt = new String(resource3.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public String getCardSuggestionPrompt() {
        return cardSuggestionPrompt;
    }

    public String getCardRewardPrompt() {
        return cardRewardPrompt;
    }

    public String getCardSuggestionJsonPrompt() {
        return cardSuggestionJsonPrompt;
    }
}
