package com.murasame.mapper;

import com.murasame.entity.SysLoginLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper {

    /** 插入登录日志 */
    @Insert("INSERT INTO sys_login_log (user_id, email, ip, user_agent, status) " +
            "VALUES (#{userId}, #{email}, #{ip}, #{userAgent}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysLoginLog loginLog);

    /** 分页查询登录日志，可按用户/邮箱/时间筛选 */
    @Select("<script>" +
            "SELECT * FROM sys_login_log WHERE 1=1 " +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='email != null and email != \"\"'>AND email LIKE CONCAT('%',#{email},'%') </if>" +
            "ORDER BY login_time DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<SysLoginLog> findAllPage(@Param("userId") Long userId,
                                  @Param("email") String email,
                                  @Param("pageSize") int pageSize,
                                  @Param("offset") int offset);

    /** 登录日志总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_login_log WHERE 1=1 " +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='email != null and email != \"\"'>AND email LIKE CONCAT('%',#{email},'%') </if>" +
            "</script>")
    long count(@Param("userId") Long userId, @Param("email") String email);
}
