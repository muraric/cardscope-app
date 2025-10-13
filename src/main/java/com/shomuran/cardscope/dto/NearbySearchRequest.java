package com.shomuran.cardscope.dto;

import lombok.Builder;
import lombok.Data;

// Class representing the POST request body for the searchNearby API
@Data
@Builder
public class NearbySearchRequest {

    private String[] includedTypes;
    private int maxResultCount;
    private LocationRestriction locationRestriction;

    @Data
    @Builder
    public static class LocationRestriction {
        private Circle circle;
    }

    @Data
    @Builder
    public static class Circle {
        private Center center;
        private double radius; // Radius in meters
    }

    @Data
    @Builder
    public static class Center {
        private double latitude;
        private double longitude;
    }
}