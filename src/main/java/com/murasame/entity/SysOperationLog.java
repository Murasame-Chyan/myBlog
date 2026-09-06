package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private Long targetId;
    private String targetName;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createTime;
}
