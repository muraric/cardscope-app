package com.shomuran.cardscope.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
public class UserProfileDto {
    private Long id;
    private String email;
    private String name;
    private String passwordHash;
    private List<CreditCardDto> userCards;
}
