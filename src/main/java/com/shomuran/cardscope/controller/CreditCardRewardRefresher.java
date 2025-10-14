package com.shomuran.cardscope.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shomuran.cardscope.model.CreditCard;
import com.shomuran.cardscope.repository.CreditCardRepository;
import com.shomuran.cardscope.service.RewardDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@EnableScheduling
public class CreditCardRewardRefresher {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private RewardDetailService rewardDetailService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * ✅ Runs automatically every 4 hours on the hour.
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void refreshEmptyRewardDetails() {
        log.info("🕓 Scheduled task: checking CreditCard table for empty reward details...");

        List<CreditCard> emptyRewardCards = creditCardRepository.findCardsWithEmptyRewards();

        if (emptyRewardCards.isEmpty()) {
            log.info("✅ No empty rewardDetails found — nothing to refresh.");
            return;
        }

        log.info("🔍 Found {} cards with empty rewardDetails. Refreshing via OpenAI...", emptyRewardCards.size());

        for (CreditCard card : emptyRewardCards) {
            String cardName = card.getIssuer() + " " + card.getCardProduct();

            rewardDetailService.getRewardDetailsAsync(cardName)
                    .thenAccept(rewardDetails -> {
                        try {
                            String rewardJson = objectMapper.writeValueAsString(rewardDetails);

                            if (rewardJson != null && rewardJson.trim().startsWith("{\"error\":")) {
                                log.error("❌ Timeout/error for {} — saving as empty {{}}", cardName);
                                rewardJson = "{}";
                            } else if (rewardJson != null && rewardJson.replaceAll("\\s+", "").equals("{}")) {
                                log.warn("⚠️ OpenAI returned empty {{}} for {}.", cardName);
                                rewardJson = "{}";
                            }

                            // ✅ UPSERT instead of save() — updates if exists, inserts if missing
                            creditCardRepository.upsertCard(
                                    card.getIssuer(),
                                    card.getCardProduct(),
                                    rewardJson
                            );

                            log.info("✅ Upserted reward details for {}", cardName);

                        } catch (Exception e) {
                            log.error("❌ Failed to serialize or upsert reward details for {}: {}", cardName, e.getMessage());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("⚠️ Async error fetching reward details for {}: {}", cardName, ex.getMessage());
                        return null;
                    });
        }
    }
}
