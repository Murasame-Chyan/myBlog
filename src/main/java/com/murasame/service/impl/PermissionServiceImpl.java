package com.murasame.service.impl;

import com.murasame.entity.SysPermission;
import com.murasame.mapper.PermissionMapper;
import com.murasame.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Set<String> getUserPermissionCodes(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return permissionMapper.findByUserId(userId)
                .stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toSet());
    }
}
