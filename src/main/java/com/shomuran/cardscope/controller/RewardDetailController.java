package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.config.PromptLoader;
import com.shomuran.cardscope.service.RewardDetailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class RewardDetailController {

    @Autowired
    private PromptLoader promptLoader;

    @Autowired
    private RewardDetailService rewardDetailService;

    @PostMapping("/cardReward")
    public Map<?, ?> getCardRewards(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> payload) {
        String cardName = (String) payload.get("card_name");
        //return ResponseEntity.ok(responseMap);
        return rewardDetailService.getRewardDetails(cardName);
    }
}