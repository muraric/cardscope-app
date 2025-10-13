package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.dto.CreditCardDto;
import com.shomuran.cardscope.dto.UserProfileDto;
import com.shomuran.cardscope.dto.UserUpdateRequestDto;
import com.shomuran.cardscope.model.CreditCard;
import com.shomuran.cardscope.model.UserCard;
import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.CreditCardRepository;
import com.shomuran.cardscope.repository.UserProfileRepository;
import com.shomuran.cardscope.service.RewardDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserProfileController {

    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private RewardDetailService rewardDetailService;
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/login")
    public ResponseEntity<UserProfile> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setEmail(email);
                    newProfile.setName(""); // optional: user can update later
                    newProfile.setUserCards(new ArrayList<>());
                    return userProfileRepository.save(newProfile);
                });

        return ResponseEntity.ok(profile);
    }

    // Create a profile
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserProfile userProfile) {
        if (userProfileRepository.existsByEmail(userProfile.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
        }
        return ResponseEntity.ok(userProfileRepository.save(userProfile));
    }

    @PutMapping("/{email}")
    public ResponseEntity<UserProfileDto> updateUserCards(
            @PathVariable String email,
            @RequestBody UserUpdateRequestDto request) {

        return userProfileRepository.findByEmail(email)
                .map(user -> {
                    // update user name if provided
                    if (request.getName() != null) {
                        user.setName(request.getName());
                    }

                    // ✅ Clear old cards before replacing
                    user.setUserCards(new ArrayList<>());

                    List<UserCard> newCardList = new ArrayList<>();

                    // process incoming cards
                    for (UserUpdateRequestDto.CardDto cardMap : request.getUserCards()) {
                        String cardName = cardMap.getIssuer() + " " + cardMap.getCardProduct();
                        if (cardName.isBlank()) continue;

                        try {
                            boolean exists = creditCardRepository
                                    .existsByIssuerIgnoreCaseAndCardProductIgnoreCase(cardMap.getIssuer(), cardMap.getCardProduct());
                            if (!exists) {
                                Map<?, ?> rewardDetails = rewardDetailService.getRewardDetails(cardName);
                                String rewardJson = objectMapper.writeValueAsString(rewardDetails);

                                CreditCard creditCard = new CreditCard();
                                creditCard.setIssuer(cardMap.getIssuer());
                                creditCard.setCardProduct(cardMap.getCardProduct());
                                creditCard.setRewardDetails(rewardJson);
                                creditCardRepository.save(creditCard);
                            }
                            newCardList.add(new UserCard(cardMap.getIssuer(), cardMap.getCardProduct()));
                        } catch (Exception e) {
                            System.out.println("❌ Failed to serialize reward details for card: " + cardName);
                        }
                    }

                    // ✅ Replace with only the new list
                    user.setUserCards(newCardList);
                    userProfileRepository.save(user);

                    // build response (same as GET)
                    List<CreditCardDto> cardDtos = user.getUserCards().stream()
                            .map(card -> {
                                String rewardStr = creditCardRepository
                                        .findByIssuerIgnoreCaseAndCardProductIgnoreCase(card.getIssuer(), card.getCardProduct())
                                        .map(CreditCard::getRewardDetails)
                                        .orElse("{}");

                                Object rewardJson;
                                try {
                                    rewardJson = objectMapper.readValue(rewardStr, Object.class);
                                } catch (Exception e) {
                                    rewardJson = new HashMap<>();
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
    }


    // Get profile by email
    @GetMapping("/{email}")
    public ResponseEntity<UserProfileDto> getUserByEmail(@PathVariable String email) {
        return userProfileRepository.findByEmail(email)
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
    }

    // Delete profile
    @DeleteMapping("/{email}")
    public ResponseEntity<?> deleteByEmail(@PathVariable String email) {
        return userProfileRepository.findByEmail(email).map(profile -> {
            userProfileRepository.delete(profile);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}