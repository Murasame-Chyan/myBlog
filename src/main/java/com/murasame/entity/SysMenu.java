package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysMenu {
    private Long id;
    private Long parentId;
    private String name;
    private String title;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer hidden;
    private Long permissionId;
    private LocalDateTime createTime;
}
