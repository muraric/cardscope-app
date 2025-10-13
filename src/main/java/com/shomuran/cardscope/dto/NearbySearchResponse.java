package com.shomuran.cardscope.dto;

import lombok.Data;
import java.util.List;

// Class representing the entire JSON response from the searchNearby API
@Data
public class NearbySearchResponse {
    private List<Place> places;

    @Data
    public static class Place {
      //
        private DisplayName displayName;
        // Add other requested fields here, e.g.,
        // private String formattedAddress;
        private String primaryType;
    }

    @Data
    public static class DisplayName {
        private String text;
        private String languageCode;
    }
}