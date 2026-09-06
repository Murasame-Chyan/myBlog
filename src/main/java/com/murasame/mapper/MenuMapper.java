package com.murasame.mapper;

import com.murasame.entity.SysMenu;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface MenuMapper {

    /** 查全部菜单，按 sort 排序 */
    @Select("SELECT * FROM sys_menu ORDER BY sort ASC, id ASC")
    List<SysMenu> findAll();

    /** 按ID查 */
    @Select("SELECT * FROM sys_menu WHERE id = #{id}")
    SysMenu findById(@Param("id") Long id);

    /** 新增 */
    @Insert("INSERT INTO sys_menu (parent_id, name, title, path, component, icon, sort, hidden, permission_id) " +
            "VALUES (#{parentId}, #{name}, #{title}, #{path}, #{component}, #{icon}, #{sort}, #{hidden}, #{permissionId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysMenu menu);

    /** 更新 */
    @Update("UPDATE sys_menu SET parent_id = #{parentId}, name = #{name}, title = #{title}, " +
            "path = #{path}, component = #{component}, icon = #{icon}, sort = #{sort}, " +
            "hidden = #{hidden}, permission_id = #{permissionId} WHERE id = #{id}")
    int update(SysMenu menu);

    /** 删除 */
    @Delete("DELETE FROM sys_menu WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** 查某角色拥有的菜单 */
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} ORDER BY m.sort ASC")
    List<SysMenu> findByRoleId(@Param("roleId") Long roleId);

    /** 查某用户拥有的菜单（通过角色间接查，去重） */
    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} ORDER BY m.sort ASC")
    List<SysMenu> findByUserId(@Param("userId") Long userId);
}
