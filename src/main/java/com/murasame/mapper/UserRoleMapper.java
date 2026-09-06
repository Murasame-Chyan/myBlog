package com.murasame.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper {

    /** 给用户分配角色 */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 移除用户的某个角色 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteByUserAndRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 移除用户的所有角色 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 查用户的角色ID列表 */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /** 查拥有某角色的用户数 */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Long roleId);
}
