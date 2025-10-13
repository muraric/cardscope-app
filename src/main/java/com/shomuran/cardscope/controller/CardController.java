package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.repository.CreditCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping("/api/cards")
public class CardController {
    @Autowired
    private CreditCardRepository creditCardRepository;

    // Example Spring Boot controller
    @GetMapping("/issuers")
    public List<String> getIssuers(@RequestParam String search) {
        return creditCardRepository.findDistinctIssuerByNameContainingIgnoreCase(search);
    }

    @GetMapping("/products")
    public List<String> getProducts(
            @RequestParam String issuer,
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        return creditCardRepository.findProductsByIssuer(issuer, search);
    }
}
