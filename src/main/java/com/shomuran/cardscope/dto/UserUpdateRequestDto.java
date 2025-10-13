package com.shomuran.cardscope.dto;

import java.util.List;

public class UserUpdateRequestDto {
    private String name;
    private List<CardDto> userCards;

    public static class CardDto {
        private String issuer;
        private String cardProduct;

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public String getCardProduct() { return cardProduct; }
        public void setCardProduct(String cardProduct) { this.cardProduct = cardProduct; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CardDto> getUserCards() { return userCards; }
    public void setUserCards(List<CardDto> userCards) { this.userCards = userCards; }
}
