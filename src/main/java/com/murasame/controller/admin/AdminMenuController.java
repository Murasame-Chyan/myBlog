package com.murasame.controller.admin;

import com.murasame.entity.SysMenu;
import com.murasame.mapper.MenuMapper;
import com.murasame.mapper.RoleMenuMapper;
import com.murasame.security.OperationLog;
import com.murasame.security.RequirePermission;
import com.murasame.util.ReturnUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/menus")
public class AdminMenuController {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public AdminMenuController(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /** 平铺列表 */
    @GetMapping
    @RequirePermission("menu:list")
    public Map<String, Object> list() {
        return ReturnUtil.success(menuMapper.findAll());
    }

    /** 树形结构（前端侧边栏渲染、菜单管理页用） */
    @GetMapping("/tree")
    @RequirePermission("menu:list")
    public Map<String, Object> tree() {
        List<SysMenu> all = menuMapper.findAll();
        List<Map<String, Object>> tree = all.stream()
                .filter(m -> m.getParentId() == 0)
                .map(m -> toNode(m, all))
                .toList();
        return ReturnUtil.success(tree);
    }

    /** 创建菜单 */
    @OperationLog(module = "菜单管理", action = "创建菜单")
    @PostMapping
    @RequirePermission("menu:create")
    public Map<String, Object> create(@RequestBody SysMenu menu) {
        if (menu.getName() == null || menu.getName().isBlank()) {
            return ReturnUtil.error("路由名称不能为空");
        }
        if (menu.getTitle() == null || menu.getTitle().isBlank()) {
            return ReturnUtil.error("菜单标题不能为空");
        }
        if (menu.getPath() == null || menu.getPath().isBlank()) {
            return ReturnUtil.error("路由路径不能为空");
        }
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getSort() == null) menu.setSort(0);
        if (menu.getHidden() == null) menu.setHidden(0);

        menuMapper.insert(menu);
        return ReturnUtil.success("菜单创建成功");
    }

    /** 更新菜单 */
    @OperationLog(module = "菜单管理", action = "更新菜单")
    @PutMapping("/{id}")
    @RequirePermission("menu:update")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        if (menuMapper.findById(id) == null) {
            return ReturnUtil.notFound("菜单不存在");
        }
        menu.setId(id);
        menuMapper.update(menu);
        return ReturnUtil.success("菜单更新成功");
    }

    /** 删除菜单（有子菜单或已被角色引用则拒绝） */
    @OperationLog(module = "菜单管理", action = "删除菜单")
    @DeleteMapping("/{id}")
    @RequirePermission("menu:delete")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (menuMapper.findById(id) == null) {
            return ReturnUtil.notFound("菜单不存在");
        }
        boolean hasChildren = menuMapper.findAll().stream()
                .anyMatch(m -> m.getParentId() != null && m.getParentId().equals(id));
        if (hasChildren) {
            return ReturnUtil.error("该菜单下有子菜单，请先删除子菜单");
        }
        // 先清空该菜单与所有角色的关联，再删除菜单本身
        roleMenuMapper.deleteByMenuId(id);
        menuMapper.deleteById(id);
        return ReturnUtil.success("菜单删除成功");
    }

    /** 将菜单转为树节点 */
    private Map<String, Object> toNode(SysMenu m, List<SysMenu> all) {
        List<Map<String, Object>> children = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId().equals(m.getId()))
                .map(c -> toNode(c, all))
                .toList();
        return Map.of(
                "id", m.getId(),
                "parentId", m.getParentId() != null ? m.getParentId() : 0,
                "name", m.getName(),
                "title", m.getTitle() != null ? m.getTitle() : "",
                "path", m.getPath() != null ? m.getPath() : "",
                "component", m.getComponent() != null ? m.getComponent() : "",
                "icon", m.getIcon() != null ? m.getIcon() : "",
                "sort", m.getSort() != null ? m.getSort() : 0,
                "hidden", m.getHidden() != null ? m.getHidden() : 0,
                "children", children
        );
    }
}
