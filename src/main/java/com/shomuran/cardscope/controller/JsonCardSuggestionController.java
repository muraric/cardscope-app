package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.config.PromptLoader;
import com.shomuran.cardscope.dto.CreditCardDto;
import com.shomuran.cardscope.dto.UserProfileDto;
import com.shomuran.cardscope.model.CreditCard;
import com.shomuran.cardscope.repository.CreditCardRepository;
import com.shomuran.cardscope.repository.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/json")
@CrossOrigin
public class JsonCardSuggestionController {

    @Autowired
    private PromptLoader promptLoader;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST endpoint that takes the user's card definitions (JSON)
     * instead of reading them from the database.
     */
    @PostMapping("/get-card-suggestions")
    public ResponseEntity<?> getCardSuggestions(@RequestBody Map<String, Object> payload) {
        try {
            // Get inputs
            String store = (String) payload.get("store");
            ResponseEntity<UserProfileDto> userCardsJson = userProfileRepository.findByEmail((String) payload.get("email"))
                    .map(user -> {
                        // Build card DTOs
                        List<CreditCardDto> cardDtos = user.getUserCards().stream()
                                .map(card -> {
                                    String rewardDetails = creditCardRepository
                                            .findByIssuerIgnoreCaseAndCardProductIgnoreCase(card.getIssuer(), card.getCardProduct())
                                            .map(CreditCard::getRewardDetails)
                                            .orElse("{}");
                                    Object rewardJson;
                                    try {
                                        rewardJson = objectMapper.readValue(rewardDetails, Object.class);
                                    } catch (Exception e) {
                                        System.out.println("Invalid Json");
                                        rewardJson = rewardDetails; // fallback if invalid JSON
                                    }
                                    return new CreditCardDto(card.getIssuer(), card.getCardProduct(), rewardJson);
                                }).toList();

                        UserProfileDto dto = new UserProfileDto(
                                user.getId(),
                                user.getEmail(),
                                user.getName(),
                                user.getPasswordHash(),
                                cardDtos
                        );
                        return ResponseEntity.ok(dto);
                    })
                    .orElse(ResponseEntity.notFound().build());
            // Load system prompt
            String basePrompt = promptLoader.getCardSuggestionJsonPrompt();
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userCardsJson));
            // Build user prompt with provided JSON
            userCardsJson.getBody().getUserCards();
            String userPrompt = "The user provided the following card definitions JSON:\n" +
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userCardsJson.getBody().getUserCards()) + "\n\n" +
                    "Store: " + store + ".\n" +
                    "Use the JSON definitions above to generate the top 3 card suggestions following the schema.";

            // Build request for OpenAI Responses API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-5"); // use the latest model
            requestBody.put("input", List.of(
                    Map.of("role", "system", "content", basePrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            System.out.println(basePrompt);
            System.out.println(userPrompt);
            // ✅ Explicitly use okhttp3.RequestBody
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    mapper.writeValueAsString(requestBody)
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/responses")
                    .header("Authorization", "Bearer " + openAiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept-Encoding", "gzip")  // enables GZIP compression
                    .post(body)
                    .build();


            String responseText;
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ResponseEntity.status(response.code())
                            .body(Map.of("error", "OpenAI API call failed: " + response.message()));
                }
                String responseBody = response.body().string();
                Map<String, Object> bodyMap = mapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) bodyMap.get("output");
                if (outputs == null || outputs.isEmpty()) {
                    return ResponseEntity.status(500).body(Map.of("error", "No output from model"));
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
            }

            if (responseText == null) {
                return ResponseEntity.status(500).body(Map.of("error", "No output_text from model"));
            }

            // Clean JSON
            responseText = responseText.trim().replaceAll("```(json)?", "").trim();
            Map<String, Object> parsedResponse = mapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
            return ResponseEntity.ok(parsedResponse);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "I/O error: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error generating suggestions: " + e.getMessage()));
        }
    }
}
