package com.murasame.mapper;

import com.murasame.entity.SysOperationLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    /** 分页查询操作日志，可按模块/用户/时间范围筛选 */
    @Select("<script>" +
            "SELECT * FROM sys_operation_log WHERE 1=1 " +
            "<if test='module != null and module != \"\"'>AND module = #{module} </if>" +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='startTime != null'>AND create_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND create_time &lt;= #{endTime} </if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<SysOperationLog> findAllPage(@Param("module") String module,
                                      @Param("userId") Long userId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("pageSize") int pageSize,
                                      @Param("offset") int offset);

    /** 统计数量（与 findAllPage 配套） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_operation_log WHERE 1=1 " +
            "<if test='module != null and module != \"\"'>AND module = #{module} </if>" +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='startTime != null'>AND create_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'>AND create_time &lt;= #{endTime} </if>" +
            "</script>")
    long count(@Param("module") String module,
               @Param("userId") Long userId,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);

    /** 插入操作日志（由 OperationLogAspect 调用） */
    @Insert("INSERT INTO sys_operation_log " +
            "(user_id, username, module, action, target_id, target_name, detail, ip, user_agent) " +
            "VALUES (#{userId}, #{username}, #{module}, #{action}, #{targetId}, #{targetName}, #{detail}, #{ip}, #{userAgent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysOperationLog log);
}
