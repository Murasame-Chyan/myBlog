package com.murasame.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SignInRecord {
    private Long id;
    private Long userId;
    private LocalDate signDate;
    private String letter;
    private Integer baseExp;
    private Integer bonusExp;
    private Integer totalExp;
    private Boolean isMakeup;
    private LocalDateTime createdAt;
}
