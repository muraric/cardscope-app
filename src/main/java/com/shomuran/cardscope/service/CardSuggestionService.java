package com.shomuran.cardscope.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shomuran.cardscope.model.CreditCard;
import com.shomuran.cardscope.model.UserCard;
import com.shomuran.cardscope.repository.CreditCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardSuggestionService {

    @Autowired
    private CreditCardRepository creditCardRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Get current quarter (Q1, Q2, Q3, Q4) based on current date
     */
    private String getCurrentQuarter() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 1 && month <= 3) return "Q1";
        if (month >= 4 && month <= 6) return "Q2";
        if (month >= 7 && month <= 9) return "Q3";
        return "Q4";
    }

    /**
     * Extract numeric value from rate string (e.g., "5%" -> 5.0, "1.5%" -> 1.5, "1% Cash Back" -> 1.0)
     * Only extracts the first number before the % sign to avoid parsing errors
     */
    private double extractRateValue(String rateStr) {
        if (rateStr == null || rateStr.isEmpty()) return 0.0;
        try {
            // Find the first number (with optional decimal) followed by %
            // Pattern: optional digits, optional decimal point, digits, then %
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)\\s*%");
            java.util.regex.Matcher matcher = pattern.matcher(rateStr);
            
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                return Double.parseDouble(numberStr);
            }
            
            // Fallback: if no % found, try to extract first number
            String cleaned = rateStr.replaceAll("[^0-9.]", "");
            if (!cleaned.isEmpty()) {
                // Take only the first number (before any non-digit after first number)
                int dotIndex = cleaned.indexOf('.');
                if (dotIndex > 0) {
                    // Has decimal point, extract up to decimal + 2 digits max
                    String beforeDot = cleaned.substring(0, dotIndex);
                    String afterDot = cleaned.substring(dotIndex + 1);
                    if (afterDot.length() > 2) {
                        afterDot = afterDot.substring(0, 2);
                    }
                    return Double.parseDouble(beforeDot + "." + afterDot);
                } else {
                    // No decimal, take first sequence of digits
                    String firstNumber = cleaned.replaceAll("\\..*", ""); // Remove everything after first dot if exists
                    if (!firstNumber.isEmpty()) {
                        return Double.parseDouble(firstNumber);
                    }
                }
            }
            
            return 0.0;
        } catch (NumberFormatException e) {
            System.err.println("Error parsing rate: " + rateStr + " - " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Normalize category name for comparison (case-insensitive, trim spaces)
     */
    private String normalizeCategory(String category) {
        if (category == null) return "";
        return category.toLowerCase().trim();
    }

    /**
     * Check if a category matches the target category (with fuzzy matching)
     */
    private boolean categoryMatches(String cardCategory, String targetCategory) {
        String normalizedCard = normalizeCategory(cardCategory);
        String normalizedTarget = normalizeCategory(targetCategory);
        
        // Exact match
        if (normalizedCard.equals(normalizedTarget)) return true;
        
        // Partial match (e.g., "grocery" matches "groceries")
        if (normalizedCard.contains(normalizedTarget) || normalizedTarget.contains(normalizedCard)) {
            return true;
        }
        
        // Common aliases and category mappings
        Map<String, List<String>> aliases = Map.of(
            "groceries", List.of("grocery", "supermarket", "supermarkets", "grocery stores"),
            "dining", List.of("restaurant", "restaurants", "food"),
            "gas", List.of("gas station", "gas stations", "fuel", "gas stations and ev charging"),
            "travel", List.of("hotel", "hotels", "airline", "airlines", "flights", "chase travel"),
            "online retail", List.of("online", "e-commerce", "internet", "amazon"),
            "streaming", List.of("streaming services", "select streaming services", "netflix", "hulu", "disney+"),
            "department stores", List.of("department store", "old navy"),
            "entertainment", List.of("live entertainment", "select live entertainment", "fitness clubs", "hair, nails and spa services")
        );
        
        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            if ((normalizedCard.equals(entry.getKey()) || entry.getValue().contains(normalizedCard)) &&
                (normalizedTarget.equals(entry.getKey()) || entry.getValue().contains(normalizedTarget))) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if store is in exclusions list
     */
    private boolean isStoreExcluded(String store, List<String> exclusions) {
        if (store == null || exclusions == null || exclusions.isEmpty()) {
            return false;
        }
        String normalizedStore = normalizeCategory(store);
        for (String exclusion : exclusions) {
            if (normalizedStore.contains(normalizeCategory(exclusion)) || 
                normalizeCategory(exclusion).contains(normalizedStore)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get reward rate for a specific category from card reward details
     */
    private double getRewardRateForCategory(Map<String, Object> cardReward, String category, String store, String currentQuarter) {
        if (cardReward == null || category == null) return 0.0;

        double bestRate = 0.0;
        String baseRateStr = (String) cardReward.get("base_rate");
        if (baseRateStr != null) {
            bestRate = extractRateValue(baseRateStr);
        }

        // Check bonus categories
        Object bonusCategoriesObj = cardReward.get("bonus_categories");
        if (bonusCategoriesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bonusCategories = (List<Map<String, Object>>) bonusCategoriesObj;
            for (Map<String, Object> bonusCat : bonusCategories) {
                String cat = (String) bonusCat.get("category");
                if (categoryMatches(cat, category)) {
                    // Check exclusions
                    Object exclusionsObj = bonusCat.get("exclusions");
                    if (exclusionsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> exclusions = (List<String>) exclusionsObj;
                        if (isStoreExcluded(store, exclusions)) {
                            continue; // Skip this bonus if store is excluded
                        }
                    }
                    String rateStr = (String) bonusCat.get("rate");
                    double rate = extractRateValue(rateStr);
                    if (rate > bestRate) {
                        bestRate = rate;
                    }
                }
            }
        }

        // Check user choice categories
        Object userChoiceObj = cardReward.get("user_choice_categories");
        if (userChoiceObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> userChoiceCategories = (List<Map<String, Object>>) userChoiceObj;
            for (Map<String, Object> userChoice : userChoiceCategories) {
                Object optionsObj = userChoice.get("options");
                if (optionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) optionsObj;
                    for (String option : options) {
                        if (categoryMatches(option, category)) {
                            // Check exclusions
                            Object exclusionsObj = userChoice.get("exclusions");
                            if (exclusionsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> exclusions = (List<String>) exclusionsObj;
                                if (isStoreExcluded(store, exclusions)) {
                                    continue; // Skip this bonus if store is excluded
                                }
                            }
                            String rateStr = (String) userChoice.get("rate");
                            double rate = extractRateValue(rateStr);
                            if (rate > bestRate) {
                                bestRate = rate;
                            }
                        }
                    }
                }
            }
        }

        // Check rotating categories for current quarter
        Object rotatingObj = cardReward.get("rotating_categories");
        if (rotatingObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rotatingCategories = (Map<String, Object>) rotatingObj;
            Object quarterCategoriesObj = rotatingCategories.get(currentQuarter);
            if (quarterCategoriesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> quarterCategories = (List<Map<String, Object>>) quarterCategoriesObj;
                for (Map<String, Object> rotCat : quarterCategories) {
                    String cat = (String) rotCat.get("category");
                    if (categoryMatches(cat, category)) {
                        // Check exclusions
                        Object exclusionsObj = rotCat.get("exclusions");
                        if (exclusionsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> exclusions = (List<String>) exclusionsObj;
                            if (isStoreExcluded(store, exclusions)) {
                                continue; // Skip this bonus if store is excluded
                            }
                        }
                        String rateStr = (String) rotCat.get("rate");
                        double rate = extractRateValue(rateStr);
                        if (rate > bestRate) {
                            bestRate = rate;
                        }
                    }
                }
            }
        }

        return bestRate;
    }

    /**
     * Calculate card score based on reward rate and other factors
     */
    private double calculateCardScore(double rewardRate, Map<String, Object> cardReward) {
        double score = rewardRate;
        
        // Bonus for higher base rate (stability factor)
        String baseRateStr = (String) cardReward.get("base_rate");
        if (baseRateStr != null) {
            double baseRate = extractRateValue(baseRateStr);
            score += baseRate * 0.1; // Small bonus for base rate
        }
        
        return score;
    }

    /**
     * Get best card suggestions based on store category from database
     */
    public Map<String, Object> getCardSuggestionsFromDatabase(
            List<UserCard> userCards,
            String store,
            String category,
            String currentQuarter) {
        
        if (userCards == null || userCards.isEmpty()) {
            return Map.of("error", "No cards found for this user");
        }

        // Use provided quarter or calculate current quarter
        if (currentQuarter == null || currentQuarter.isEmpty()) {
            currentQuarter = getCurrentQuarter();
        }

        List<Map<String, Object>> cardScores = new ArrayList<>();

        // Process each user card
        for (UserCard userCard : userCards) {
            try {
                // Fetch card details from database
                Optional<CreditCard> creditCardOpt = creditCardRepository
                        .findByIssuerIgnoreCaseAndCardProductIgnoreCase(
                                userCard.getIssuer(),
                                userCard.getCardProduct());

                if (creditCardOpt.isEmpty()) {
                    continue; // Skip if card not found in database
                }

                CreditCard creditCard = creditCardOpt.get();
                String rewardDetailsJson = creditCard.getRewardDetails();

                if (rewardDetailsJson == null || rewardDetailsJson.isEmpty() || 
                    rewardDetailsJson.trim().equals("{}")) {
                    continue; // Skip if no reward details
                }

                // Parse reward details JSON
                Map<String, Object> rewardDetails = mapper.readValue(
                        rewardDetailsJson,
                        new TypeReference<Map<String, Object>>() {});

                Map<String, Object> cardReward = (Map<String, Object>) rewardDetails.get("cardReward");
                if (cardReward == null) {
                    continue;
                }
                
                // Debug: Log base rate for troubleshooting
                String baseRateStr = (String) cardReward.get("base_rate");
                System.out.println("DEBUG - Card: " + userCard.getIssuer() + " " + userCard.getCardProduct() + 
                        " | Base Rate: " + baseRateStr);

                // Get reward rate for the category (pass store name to check exclusions)
                double rewardRate = getRewardRateForCategory(cardReward, category, store, currentQuarter);
                
                // Debug logging
                System.out.println("Card: " + userCard.getIssuer() + " " + userCard.getCardProduct() + 
                        " | Category: " + category + " | Store: " + store + 
                        " | Reward Rate: " + rewardRate + "%");
                
                if (rewardRate > 0) {
                    double score = calculateCardScore(rewardRate, cardReward);
                    
                    Map<String, Object> cardSuggestion = new HashMap<>();
                    cardSuggestion.put("issuer", userCard.getIssuer());
                    cardSuggestion.put("cardProduct", userCard.getCardProduct());
                    cardSuggestion.put("rewardRate", String.format("%.1f%%", rewardRate));
                    cardSuggestion.put("score", score);
                    cardSuggestion.put("reasoning", String.format(
                            "Best reward rate (%.1f%%) for %s category", rewardRate, category));
                    
                    cardScores.add(cardSuggestion);
                }

            } catch (Exception e) {
                // Log error but continue processing other cards
                System.err.println("Error processing card " + userCard.getIssuer() + 
                        " " + userCard.getCardProduct() + ": " + e.getMessage());
            }
        }

        // Sort by score (descending) and get top 3
        List<Map<String, Object>> topSuggestions = cardScores.stream()
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("score"),
                        (Double) a.get("score")))
                .limit(3)
                .collect(Collectors.toList());

        // Remove score from final response
        topSuggestions.forEach(suggestion -> suggestion.remove("score"));

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("store", store);
        response.put("category", category);
        response.put("currentQuarter", currentQuarter);
        response.put("suggestions", topSuggestions.isEmpty() ? 
                List.of(Map.of("error", "No cards found with reward details for this category")) : 
                topSuggestions);

        return response;
    }
}
