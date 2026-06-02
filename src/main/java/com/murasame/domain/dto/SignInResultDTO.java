package com.murasame.domain.dto;

import lombok.Data;

@Data
public class SignInResultDTO {
    private String letter;
    private int letterGroup;
    private int baseExp;
    private int bonusExp;
    private int totalExp;
    private int newExp;
    private int newLevel;
    private boolean leveledUp;
    private int consecutiveDays;
    private boolean isMakeup;
}
