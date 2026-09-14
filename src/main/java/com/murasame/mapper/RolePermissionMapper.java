package com.murasame.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper {

    /** 给角色添加一个权限 */
    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /** 批量插入（用于权限分配） */
    @Insert("<script>" +
            "INSERT INTO sys_role_permission (role_id, permission_id) VALUES " +
            "<foreach collection='permissionIds' item='permId' separator=','>" +
            "(#{roleId}, #{permId})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /** 删除角色的所有权限 */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除所有引用某权限的关联（权限删除时用） */
    @Delete("DELETE FROM sys_role_permission WHERE permission_id = #{permissionId}")
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    /** 查角色的权限ID列表 */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
