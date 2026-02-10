package com.shomuran.cardscope.service;

import com.shomuran.cardscope.dto.NearbySearchRequest;
import com.shomuran.cardscope.dto.NearbySearchResponse;
import com.shomuran.cardscope.dto.StoreInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GooglePlacesService {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PLACES_API_URL = "https://places.googleapis.com/v1/places:searchNearby";

    /**
     * Detect nearest store and category using latitude & longitude.
     */

    public List<StoreInfo> detectNearbyStores(double latitude, double longitude) {
        String placesUrl = String.format(
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=100&key=%s",
                latitude, longitude, googleApiKey
        );

        Map<String, Object> response = restTemplate.getForObject(placesUrl, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) return List.of();

        return results.stream()
                .limit(5) // top 5
                .map(r -> {
                    String storeName = (String) r.get("name");
                    String category = getCategoryForStore(storeName); // reuse your logic
                    return new StoreInfo(storeName, category);
                })
                .toList();
    }

    /**
     * Detect nearest store and category using latitude & longitude.
     */
    public StoreInfo detectNearestStore(double latitude, double longitude) {
        String placesUrl = String.format(
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=100&type=store&key=%s",
                latitude, longitude, googleApiKey
        );

        Map<String, Object> response = restTemplate.getForObject(placesUrl, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) {
            return new StoreInfo("Unknown Store", "general");
        }

        Map<String, Object> firstResult = results.get(0);

        String storeName = (String) firstResult.get("name");

        // Extract Google category if available
        List<String> types = (List<String>) firstResult.get("types");
        String category = (types != null && !types.isEmpty()) ? types.get(0) : getCategoryForStore(storeName);

        return new StoreInfo(storeName, category);
    }
    /**
     * Fallback category mapping by store name keywords.
     */
    /**
     * Use Google Places Text Search to resolve store category for a given store name.
     */

/*
    public String getCategoryForStore(String storeName) {
        if (storeName == null) return "general";

        String lower = storeName.toLowerCase();
        if (lower.contains("walmart") || lower.contains("target")) return "department_store";
        if (lower.contains("amazon") || lower.contains("etsy")) return "online";
        if (lower.contains("kroger") || lower.contains("whole foods")) return "groceries";

        return "general";
    }
*/
    public String getCategoryForStore(String storeName) {
        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s",
                    storeName.replace(" ", "+"), googleApiKey
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) return "general";

            // Take first match
            Map<String, Object> firstResult = results.get(0);
            List<String> types = (List<String>) firstResult.get("types");

            if (types != null && !types.isEmpty()) {
                return types.get(0); // return the first type as category
            }

            return "general";
        } catch (Exception e) {
            e.printStackTrace();
            return "general";
        }
    }

    public NearbySearchResponse detectNearestStorev2(double latitude, double longitude) {
        // 1. Setup Request Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        // This is a REQUIRED header for the Places API (New)
        headers.set("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location,places.primaryType");

        // 2. Create the Request Entity (Headers + Body)
        String[] includeTypes = {"restaurant", "cafe", "bakery", "bar", "night_club", "clothing_store", "supermarket", "book_store", "shopping_mall", "atm", "accounting", "gas_station", "car_dealer", "car_repair", "parking", "car_wash", "hospital", "dentist", "doctor", "pharmacy", "physiotherapist", "lodging", "rv_park", "university", "school", "library", "primary_school", "hair_care", "laundry", "travel_agency", "park", "zoo", "stadium", "gym", "museum"};
        NearbySearchRequest.Center center = NearbySearchRequest.Center.builder()
                .latitude(latitude) // Set the first field
                .longitude(longitude) // Set the second field on the SAME builder
                .build(); // Build the final object
        NearbySearchRequest.Circle circle =NearbySearchRequest.Circle.builder()
                .radius(500)
                .center(center)
                .build();
        NearbySearchRequest.LocationRestriction locationRestriction = NearbySearchRequest.LocationRestriction.builder()
                .circle(circle)
                .build();
        NearbySearchRequest requestBody = NearbySearchRequest.builder()
                .maxResultCount(10)
                .includedTypes(includeTypes)
                .locationRestriction(locationRestriction)
                .build();

        HttpEntity<NearbySearchRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Send the POST Request using RestTemplate
            ResponseEntity<NearbySearchResponse> response = restTemplate.postForEntity(
                    PLACES_API_URL,
                    entity,
                    NearbySearchResponse.class
            );

            // 4. Return the response body
            return response.getBody();

        } catch (Exception e) {
            // Log the error and throw a custom exception or return an empty response
            System.err.println("Error calling Google Places API: " + e.getMessage());
            throw new RuntimeException("Nearby search failed", e);
        }
    }

}
