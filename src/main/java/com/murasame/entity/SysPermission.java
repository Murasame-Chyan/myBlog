package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermission {
    private Long id;
    private String code;
    private String name;
    private Integer type;
    private Long parentId;
    private LocalDateTime createTime;
}
