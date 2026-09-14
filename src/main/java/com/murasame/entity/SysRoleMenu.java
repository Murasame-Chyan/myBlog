package com.murasame.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRoleMenu {
    private Long id;
    private Long roleId;
    private Long menuId;
    private LocalDateTime createTime;
}
