package com.shomuran.cardscope.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class HealthPingJob {

    private static final Logger log = LoggerFactory.getLogger(HealthPingJob.class);
    private final WebClient webClient;

    // Base URL of THIS backend (adjust for your cloud hostname when deployed)
    public HealthPingJob(@Value("${app.baseUrl:http://localhost:8080}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // Run every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void pingSelfHealth() {
        webClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(body -> log.info("Health OK: {}", body))
                .doOnError(err -> log.error("Health check FAILED", err))
                .onErrorResume(err -> Mono.empty())
                .subscribe();
    }
}
