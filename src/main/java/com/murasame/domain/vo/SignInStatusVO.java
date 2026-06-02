package com.murasame.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class SignInStatusVO {
    private boolean loggedIn;
    private boolean signedToday;
    private int consecutiveDays;
    private List<String> missedDays;
    private boolean canMakeup;
    private String todayLetter;
    private int totalSignIns;
    private int currentExp;
    private int currentLevel;
    private int expForNextLevel;
    private int expProgressInLevel;
    private int currentLevelExp;
    private boolean isMaxLevel;
}
