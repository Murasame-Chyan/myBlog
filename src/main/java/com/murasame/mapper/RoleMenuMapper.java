package com.murasame.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色-菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper {

    /** 给角色添加一个菜单 */
    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insert(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /** 批量插入 */
    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleId}, #{menuId})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /** 删除角色的所有菜单 */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除所有引用某菜单的关联（菜单删除时用） */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    /** 查角色的菜单ID列表 */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);
}
