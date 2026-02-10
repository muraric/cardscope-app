package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.dto.StoreInfo;
import com.shomuran.cardscope.model.UserCard;
import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.UserProfileRepository;
import com.shomuran.cardscope.service.GooglePlacesService;
import com.shomuran.cardscope.service.CardSuggestionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private CardSuggestionService cardSuggestionService;

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

            // Use database-based card suggestion service instead of ChatGPT
            Map<String, Object> responseMap = cardSuggestionService.getCardSuggestionsFromDatabase(
                    userCards,
                    store,
                    category,
                    currentQuarter
            );

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error generating suggestions: " + e.getMessage()));
        }
    }
}