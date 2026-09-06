package com.murasame.mapper;

import com.murasame.entity.SysPermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 权限码 Mapper
 */
@Mapper
public interface PermissionMapper {

    /** 查全部权限码 */
    @Select("SELECT * FROM sys_permission ORDER BY parent_id ASC, id ASC")
    List<SysPermission> findAll();

    /** 按ID查 */
    @Select("SELECT * FROM sys_permission WHERE id = #{id}")
    SysPermission findById(@Param("id") Long id);

    /** 按权限标识查 */
    @Select("SELECT * FROM sys_permission WHERE code = #{code}")
    SysPermission findByCode(@Param("code") String code);

    /** 新增 */
    @Insert("INSERT INTO sys_permission (code, name, type, parent_id) " +
            "VALUES (#{code}, #{name}, #{type}, #{parentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysPermission permission);

    /** 更新 */
    @Update("UPDATE sys_permission SET code = #{code}, name = #{name}, " +
            "type = #{type}, parent_id = #{parentId} WHERE id = #{id}")
    int update(SysPermission permission);

    /** 删除 */
    @Delete("DELETE FROM sys_permission WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** 查某角色拥有的权限码 */
    @Select("SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<SysPermission> findByRoleId(@Param("roleId") Long roleId);

    /** 查某用户的所有权限码（通过角色间接查） */
    @Select("SELECT DISTINCT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<SysPermission> findByUserId(@Param("userId") Long userId);
}
