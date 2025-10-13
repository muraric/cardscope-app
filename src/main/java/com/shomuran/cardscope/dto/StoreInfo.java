package com.shomuran.cardscope.dto;

public class StoreInfo {
    private String name;
    private String category;

    public StoreInfo(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
}
