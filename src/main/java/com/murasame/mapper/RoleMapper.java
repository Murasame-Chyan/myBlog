package com.murasame.mapper;

import com.murasame.entity.SysRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper {

    /** 查询所有启用的角色，按 sort 排序 */
    @Select("SELECT * FROM sys_role WHERE status = 1 ORDER BY sort ASC")
    List<SysRole> findAll();

    /** 按ID查角色 */
    @Select("SELECT * FROM sys_role WHERE id = #{id}")
    SysRole findById(@Param("id") Long id);

    /** 按编码查角色（如 ROLE_SUPER_ADMIN） */
    @Select("SELECT * FROM sys_role WHERE code = #{code}")
    SysRole findByCode(@Param("code") String code);

    /** 新增角色，返回自增ID */
    @Insert("INSERT INTO sys_role (name, code, data_scope, status, sort) " +
            "VALUES (#{name}, #{code}, #{dataScope}, #{status}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRole role);

    /** 更新角色 */
    @Update("UPDATE sys_role SET name = #{name}, code = #{code}, " +
            "data_scope = #{dataScope}, status = #{status}, sort = #{sort} " +
            "WHERE id = #{id}")
    int update(SysRole role);

    /** 删除角色 */
    @Delete("DELETE FROM sys_role WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** 查询某用户拥有的角色列表 */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<SysRole> findByUserId(@Param("userId") Long userId);
}
