package com.shomuran.cardscope.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class UserCard {

    private String issuer;
    private String cardProduct;

    public UserCard() {}

    public UserCard(String issuer, String cardProduct) {
        this.issuer = issuer;
        this.cardProduct = cardProduct;
    }

    public String getIssuer() {
        return issuer;
    }
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCardProduct() {
        return cardProduct;
    }
    public void setCardProduct(String cardProduct) {
        this.cardProduct = cardProduct;
    }
}
