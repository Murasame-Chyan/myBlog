package com.murasame.service;

import java.util.Set;

/**
 * 权限查询服务 — 从 DB 查出用户权限码集合
 */
public interface PermissionService {

    /**
     * 获取用户的所有权限码
     * @param userId 用户ID
     * @return 权限码集合，如 {"dashboard:view", "blog:delete", ...}
     */
    Set<String> getUserPermissionCodes(Long userId);
}
