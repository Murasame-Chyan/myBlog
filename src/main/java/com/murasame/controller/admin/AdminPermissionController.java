package com.murasame.controller.admin;

import com.murasame.entity.SysPermission;
import com.murasame.mapper.PermissionMapper;
import com.murasame.mapper.RolePermissionMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/permissions")
public class AdminPermissionController {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public AdminPermissionController(PermissionMapper permissionMapper,
                                    RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /** 平铺列表 */
    @GetMapping
    @RequirePermission("permission:list")
    public Map<String, Object> list() {
        return ReturnUtil.success(permissionMapper.findAll());
    }

    /** 树形结构（前端树形勾选用） */
    @GetMapping("/tree")
    @RequirePermission("permission:list")
    public Map<String, Object> tree() {
        List<SysPermission> all = permissionMapper.findAll();
        List<Map<String, Object>> tree = all.stream()
                .filter(p -> p.getParentId() == 0)
                .map(p -> toNode(p, all))
                .toList();
        return ReturnUtil.success(tree);
    }

    /** 创建权限码 */
    @OperationLog(module = "权限管理", action = "创建权限")
    @PostMapping
    @RequirePermission("permission:create")
    public Map<String, Object> create(@RequestBody SysPermission permission) {
        if (permission.getCode() == null || permission.getCode().isBlank()) {
            return ReturnUtil.error("权限标识不能为空");
        }
        if (permission.getName() == null || permission.getName().isBlank()) {
            return ReturnUtil.error("权限名称不能为空");
        }
        if (permissionMapper.findByCode(permission.getCode()) != null) {
            return ReturnUtil.error("权限标识已存在");
        }
        if (permission.getType() == null) permission.setType(3);
        if (permission.getParentId() == null) permission.setParentId(0L);

        permissionMapper.insert(permission);
        return ReturnUtil.success("权限创建成功");
    }

    /** 更新权限码 */
    @OperationLog(module = "权限管理", action = "更新权限")
    @PutMapping("/{id}")
    @RequirePermission("permission:update")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysPermission permission) {
        if (permissionMapper.findById(id) == null) {
            return ReturnUtil.notFound("权限不存在");
        }
        if (permission.getCode() == null || permission.getCode().isBlank()) {
            return ReturnUtil.error("权限标识不能为空");
        }
        SysPermission existing = permissionMapper.findByCode(permission.getCode());
        if (existing != null && !existing.getId().equals(id)) {
            return ReturnUtil.error("权限标识已存在");
        }
        permission.setId(id);
        permissionMapper.update(permission);
        return ReturnUtil.success("权限更新成功");
    }

    /** 删除权限码（有子节点或已被角色引用则拒绝） */
    @OperationLog(module = "权限管理", action = "删除权限")
    @DeleteMapping("/{id}")
    @RequirePermission("permission:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (permissionMapper.findById(id) == null) {
            return ReturnUtil.notFound("权限不存在");
        }
        // 有子权限不能删
        boolean hasChildren = permissionMapper.findAll().stream()
                .anyMatch(p -> p.getParentId() != null && p.getParentId().equals(id));
        if (hasChildren) {
            return ReturnUtil.error("该权限下有子权限，请先删除子权限");
        }
        // 先清空该权限与所有角色的关联，再删除权限本身
        rolePermissionMapper.deleteByPermissionId(id);
        permissionMapper.deleteById(id);
        return ReturnUtil.success("权限删除成功");
    }

    /** 将权限转为树节点 */
    private Map<String, Object> toNode(SysPermission p, List<SysPermission> all) {
        List<Map<String, Object>> children = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId().equals(p.getId()))
                .map(c -> toNode(c, all))
                .toList();
        return Map.of(
                "id", p.getId(),
                "code", p.getCode(),
                "name", p.getName(),
                "type", p.getType() != null ? p.getType() : 3,
                "parentId", p.getParentId() != null ? p.getParentId() : 0,
                "children", children
        );
    }
}
