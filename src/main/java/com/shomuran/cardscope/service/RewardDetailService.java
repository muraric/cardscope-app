package com.shomuran.cardscope.service;

import com.shomuran.cardscope.repository.UserProfileRepository;
import com.shomuran.cardscope.config.PromptLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class RewardDetailService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PromptLoader promptLoader;

    @Value("${openai.api.key}")
    private String openAiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)   // default 10s → now 60s
            .writeTimeout(120, TimeUnit.SECONDS)    // for large prompt uploads
            .readTimeout(180, TimeUnit.SECONDS)     // for long generation
            .callTimeout(180, TimeUnit.SECONDS)     // total time allowed for call
            .build();

    /**
     * Synchronous OpenAI call
     */
    public Map<?, ?> getRewardDetails(String cardName) {
        try {
            String basePrompt = promptLoader.getCardRewardPrompt();
            String userPrompt = "The user has these cards: " + cardName;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-5-mini");
            requestBody.put("tools", List.of(Map.of("type", "web_search_preview")));
            requestBody.put("input", List.of(
                    Map.of("role", "system", "content", basePrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    mapper.writeValueAsString(requestBody)
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/responses")
                    .header("Authorization", "Bearer " + openAiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            String responseText;
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return Map.of("error", "OpenAI API call failed: " + response.message());
                }

                String bodyString = response.body().string();
                Map<String, Object> bodyMap = mapper.readValue(bodyString, new TypeReference<>() {
                });

                List<Map<String, Object>> outputs = (List<Map<String, Object>>) bodyMap.get("output");
                if (outputs == null || outputs.isEmpty()) {
                    return Map.of("error", "No output from model");
                }

                responseText = null;
                for (Map<String, Object> outputItem : outputs) {
                    List<Map<String, Object>> contentList = (List<Map<String, Object>>) outputItem.get("content");
                    if (contentList != null) {
                        for (Map<String, Object> contentItem : contentList) {
                            if ("output_text".equals(contentItem.get("type"))) {
                                responseText = (String) contentItem.get("text");
                                break;
                            }
                        }
                    }
                    if (responseText != null) break;
                }

                if (responseText == null) {
                    return Map.of("error", "No output_text from model");
                }
            }

            responseText = responseText.trim();
            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("```(json)?", "").trim();
            }

            Map<String, Object> parsedResponse =
                    mapper.readValue(responseText, new TypeReference<>() {
                    });
            Map<String, Object> parsedRewards =
                    (Map<String, Object>) parsedResponse.get("cardReward");

            return Map.of("cardReward", parsedRewards);

        } catch (IOException e) {
            e.printStackTrace();
            return Map.of("error", "OpenAI API I/O error: timeout"); // standardize timeout error
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error generating suggestions: " + e.getMessage());
        }
    }

    /**
     * ✅ Async version with retry mechanism (non-blocking)
     */
    @Async("cardScopeExecutor")
    public CompletableFuture<Map<?, ?>> getRewardDetailsAsync(String cardName) {
        return CompletableFuture.supplyAsync(() -> {
            int maxRetries = 3;
            int attempt = 0;

            while (attempt < maxRetries) {
                attempt++;
                Map<?, ?> result = getRewardDetails(cardName);

                if (!isTimeoutError(result)) {
                    return result;
                }

                System.out.println("⚠️ Timeout on attempt " + attempt +
                        " for " + cardName + " — retrying...");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("❌ All retries failed for " + cardName);
            return Map.of("error", "Failed after " + maxRetries + " retries (timeout)");
        });
    }

    /**
     * Utility: check if result indicates a timeout
     */
    private boolean isTimeoutError(Map<?, ?> response) {
        return response != null && "OpenAI API I/O error: timeout".equals(response.get("error"));
    }
}
