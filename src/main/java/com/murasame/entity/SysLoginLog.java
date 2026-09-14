package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
public class SysLoginLog {
    private Long id;
    private Long userId;
    private String email;
    private String ip;
    private String userAgent;
    /** 登录结果：1=成功 0=失败 */
    private Integer status;
    private LocalDateTime loginTime;
}
