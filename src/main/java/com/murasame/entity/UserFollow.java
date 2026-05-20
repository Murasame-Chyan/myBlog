package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFollow {
    Long id;
    Long followerId;
    Long followeeId;
    LocalDateTime createdAt;
}
