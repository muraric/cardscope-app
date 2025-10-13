package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.dto.NearbySearchResponse;
import com.shomuran.cardscope.dto.StoreInfo;
import com.shomuran.cardscope.service.GooglePlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@CrossOrigin
public class GooglePlacesController {

    @Autowired
    private GooglePlacesService googlePlacesService;

    @PostMapping("/detect-store")
    public ResponseEntity<?> detectStore(@RequestBody Map<String, Object> payload) {
        Double latitude = (Double) payload.get("latitude");
        Double longitude = (Double) payload.get("longitude");

        if (latitude == null || longitude == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Latitude and longitude are required"));
        }

        String store = String.valueOf(googlePlacesService.detectNearestStore(latitude, longitude));
        String category = store != null ? googlePlacesService.getCategoryForStore(store) : null;

        return ResponseEntity.ok(Map.of(
                "store", store,
                "category", category
        ));
    }
    @GetMapping("/detect-stores")
    public ResponseEntity<?> detectStores(@RequestParam double latitude,
                                          @RequestParam double longitude) {
        List<StoreInfo> stores = googlePlacesService.detectNearbyStores(latitude, longitude);
        return ResponseEntity.ok(Map.of("stores", stores));
    }

    @GetMapping("/detect-stores-v1")
    public ResponseEntity<?> detectNearestStorev2(@RequestParam double latitude,
                                                  @RequestParam double longitude) {
        NearbySearchResponse stores = googlePlacesService.detectNearestStorev2(latitude, longitude);
        return ResponseEntity.ok(Map.of("stores", stores));
    }
}
