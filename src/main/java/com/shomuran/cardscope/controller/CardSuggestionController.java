package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.dto.StoreInfo;
import com.shomuran.cardscope.model.UserCard;
import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.UserProfileRepository;
import com.shomuran.cardscope.service.GooglePlacesService;
import com.shomuran.cardscope.config.PromptLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CardSuggestionController {

    @Autowired
    private GooglePlacesService googlePlacesService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PromptLoader promptLoader;

    @Value("${openai.api.key}")
    private String openAiKey;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/get-card-suggestions")
    public ResponseEntity<?> getCardSuggestions(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");

            // Fetch user cards from DB
            List<UserCard> userCards = userProfileRepository.findByEmail(email)
                    .map(UserProfile::getUserCards)
                    .orElse(new ArrayList<>());
            if (userCards.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No cards found for this user"));
            }

            String store = (String) payload.get("store");
            String category = (String) payload.get("category");
            String currentQuarter = (String) payload.get("currentQuarter");

            // Handle auto-detect via Google Places
            if ((store == null || store.isEmpty()) &&
                    payload.containsKey("latitude") && payload.containsKey("longitude")) {
                double latitude = Double.parseDouble(payload.get("latitude").toString());
                double longitude = Double.parseDouble(payload.get("longitude").toString());

                StoreInfo detected = googlePlacesService.detectNearestStore(latitude, longitude);
                if (detected != null) {
                    store = detected.getName();
                    category = detected.getCategory();
                }
            }

            if (store != null && category == null) {
                category = googlePlacesService.getCategoryForStore(store);
            }

            if (store == null || store.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Store name or location required"));
            }

            // Load base system prompt
            String basePrompt = promptLoader.getCardSuggestionPrompt();

            // 🔹 Combine issuer and cardProduct into a single string list
            List<String> cardNames = userCards.stream()
                    .map(uc -> uc.getIssuer() + " " + uc.getCardProduct())
                    .toList();

            // Build dynamic user context
            String userPrompt = "The user has these cards: " + String.join(", ", cardNames) + ".\n" +
                    "Store: " + store + ".\n" +
                    (category != null ? "Category: " + category + ".\n" : "") +
                    (currentQuarter != null ? "Current quarter: " + currentQuarter + ".\n" : "");
            System.out.println("userPrompt:" + userPrompt);
            // Build request payload for Responses API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4.1"); // or gpt-4o
            requestBody.put("tools", List.of(Map.of("type", "web_search_preview"))); // enable web search
            requestBody.put("input", List.of(
                    Map.of("role", "system", "content", basePrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            // ✅ Explicitly use okhttp3.RequestBody
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
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
                    return ResponseEntity.status(response.code())
                            .body(Map.of("error", "OpenAI API call failed: " + response.message()));
                }
                String bodyString = response.body().string();

                // Parse full API response into a map
                Map<String, Object> bodyMap = mapper.readValue(bodyString, new TypeReference<Map<String, Object>>() {});

                // The "output" array contains assistant messages
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) bodyMap.get("output");
                if (outputs == null || outputs.isEmpty()) {
                    return ResponseEntity.status(500).body(Map.of("error", "No output from model"));
                }

                // ✅ Scan all outputs for first "output_text"
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
                    return ResponseEntity.status(500).body(Map.of("error", "No output_text from model"));
                }
            }

            // ✅ Cleanup: strip markdown fences if present
            responseText = responseText.trim();
            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("```(json)?", "").trim();
            }
            System.out.println(responseText);
            // Parse GPT JSON into Map
            Map<String, Object> parsedResponse = mapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});
            String categoryFromAi = (String) parsedResponse.get("category");
            String quarterFromAi = (String) parsedResponse.get("currentQuarter");
            List<Map<String, Object>> parsedSuggestions = (List<Map<String, Object>>) parsedResponse.get("suggestions");

            // Build response
            Map<String, Object> responseMap = Map.of(
                    "store", store,
                    "category", categoryFromAi,
                    "currentQuarter", quarterFromAi,
                    "suggestions", parsedSuggestions
            );

            return ResponseEntity.ok(responseMap);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "OpenAI API I/O error: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error generating suggestions: " + e.getMessage()));
        }
    }
}