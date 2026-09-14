package com.murasame.controller.admin;

import com.murasame.entity.SysRole;
import com.murasame.mapper.RoleMapper;
import com.murasame.mapper.RoleMenuMapper;
import com.murasame.mapper.RolePermissionMapper;
import com.murasame.mapper.UserRoleMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {
    private final RoleMapper roleMapper;                // 角色 CRUD
    private final RolePermissionMapper rolePermissionMapper;  // 角色-权限 关联
    private final RoleMenuMapper roleMenuMapper;              // 角色-菜单 关联
    private final UserRoleMapper userRoleMapper;              // 查角色下有没有用户

    public AdminRoleController(RoleMapper roleMapper,
                               RolePermissionMapper rolePermissionMapper,
                               RoleMenuMapper roleMenuMapper,
                               UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /** 角色列表 */
    @GetMapping
    @RequirePermission("role:list")
    public Map<String, Object> list() {
        List<SysRole> roleList = roleMapper.findAll();
        return ReturnUtil.success(roleList);
    }

    /** 角色详情（含已分配的权限ID、菜单ID） */
    @GetMapping("/{id}")
    @RequirePermission("role:list")
    public Map<String, Object> getById(@PathVariable Long id) {
        SysRole role = roleMapper.findById(id);
        if (role == null) {
            return ReturnUtil.notFound("角色不存在");
        }
        List<Long> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(id);
        List<Long> menuIds = roleMenuMapper.findMenuIdsByRoleId(id);
        return ReturnUtil.success(Map.of(
                "role", role,
                "permissionIds", permissionIds,
                "menuIds", menuIds
        ));
    }

    /** 创建角色 */
    @OperationLog(module = "角色管理", action = "创建角色")
    @PostMapping
    @RequirePermission("role:create")
    public Map<String, Object> create(@RequestBody SysRole role) {
        if (role.getName() == null || role.getName().isBlank()) {
            return ReturnUtil.error("角色名称不能为空");
        }
        if (role.getCode() == null || role.getCode().isBlank()) {
            return ReturnUtil.error("角色编码不能为空");
        }
        if (roleMapper.findByCode(role.getCode()) != null) {
            return ReturnUtil.error("角色编码已存在");
        }
        // 默认值兜底
        if (role.getDataScope() == null) role.setDataScope(1);
        if (role.getStatus() == null) role.setStatus(1);
        if (role.getSort() == null) role.setSort(0);

        roleMapper.insert(role);
        return ReturnUtil.success("角色创建成功");
    }

    /** 更新角色 */
    @OperationLog(module = "角色管理", action = "更新角色")
    @PutMapping("/{id}")
    @RequirePermission("role:update")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysRole role) {
        if (roleMapper.findById(id) == null) {
            return ReturnUtil.notFound("角色不存在");
        }
        if (role.getName() == null || role.getName().isBlank()) {
            return ReturnUtil.error("角色名称不能为空");
        }
        if (role.getCode() == null || role.getCode().isBlank()) {
            return ReturnUtil.error("角色编码不能为空");
        }
        // 查重时排除自身
        SysRole existing = roleMapper.findByCode(role.getCode());
        if (existing != null && !existing.getId().equals(id)) {
            return ReturnUtil.error("角色编码已存在");
        }
        role.setId(id);
        roleMapper.update(role);
        return ReturnUtil.success("角色更新成功");
    }

    /** 删除角色（有用户占用则拒绝） */
    @OperationLog(module = "角色管理", action = "删除角色")
    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (userRoleMapper.countByRoleId(id) > 0) {
            return ReturnUtil.error("该角色下仍有用户，无法删除");
        }
        rolePermissionMapper.deleteByRoleId(id);
        roleMenuMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
        return ReturnUtil.success("角色删除成功");
    }

    /** 分配权限（全量替换） */
    @OperationLog(module = "角色管理", action = "分配权限")
    @PutMapping("/{id}/permissions")
    @RequirePermission("role:assign-perm")
    public Map<String, Object> assignPermissions(@PathVariable Long id,
                                                 @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        if (permissionIds == null) {
            return ReturnUtil.error("permissionIds 不能为空");
        }
        // 全量替换：先清空再批量插入
        rolePermissionMapper.deleteByRoleId(id);
        if (!permissionIds.isEmpty()) {
            rolePermissionMapper.insertBatch(id, permissionIds);
        }
        return ReturnUtil.success("权限分配成功");
    }

    /** 分配菜单（全量替换） */
    @OperationLog(module = "角色管理", action = "分配菜单")
    @PutMapping("/{id}/menus")
    @RequirePermission("role:assign-menu")
    public Map<String, Object> assignMenus(@PathVariable Long id,
                                           @RequestBody Map<String, List<Long>> body) {
        List<Long> menuIds = body.get("menuIds");
        if (menuIds == null) {
            return ReturnUtil.error("menuIds 不能为空");
        }
        // 全量替换：先清空再批量插入
        roleMenuMapper.deleteByRoleId(id);
        if (!menuIds.isEmpty()) {
            roleMenuMapper.insertBatch(id, menuIds);
        }
        return ReturnUtil.success("菜单分配成功");
    }
}
