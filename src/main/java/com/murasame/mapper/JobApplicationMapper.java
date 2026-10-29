package com.murasame.mapper;

import com.murasame.entity.JobApplication;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 求职投递主记录 Mapper。
 * 约定：
 * 1. 所有查询都强制带 u_id（由 Service 传入当前登录用户 id），不提供"只按 id"的入口，从源头杜绝越权；
 * 2. 列表/详情默认过滤 deleted_at IS NULL（软删除）；
 * 3. keyword/status/channel 由 Service 归一化为 null 后再传入（空字符串不参与动态条件）。
 */
@Mapper
public interface JobApplicationMapper {

    @Insert("INSERT INTO job_application (u_id, company, `position`, channel, location, job_url, applied_at, `status`, priority, salary_range, resume_version, contact, next_action, follow_up_at, notes, created_at, updated_at) " +
            "VALUES (#{uId}, #{company}, #{position}, #{channel}, #{location}, #{jobUrl}, #{appliedAt}, #{status}, #{priority}, #{salaryRange}, #{resumeVersion}, #{contact}, #{nextAction}, #{followUpAt}, #{notes}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobApplication application);

    @Select("SELECT * FROM job_application WHERE id = #{id} AND u_id = #{userId} AND deleted_at IS NULL")
    JobApplication selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM job_application " +
            "WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "<if test='keyword != null'>AND (company LIKE CONCAT('%',#{keyword},'%') OR `position` LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND `status` = #{status} </if>" +
            "<if test='channel != null'>AND channel = #{channel} </if>" +
            "<if test='overdue'>AND follow_up_at IS NOT NULL AND follow_up_at &lt; #{today} AND `status` NOT IN ('offer','rejected','dropped','archived') </if>" +
            "ORDER BY ${orderBy}, id DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<JobApplication> selectPage(@Param("userId") Long userId,
                                    @Param("keyword") String keyword,
                                    @Param("status") String status,
                                    @Param("channel") String channel,
                                    @Param("overdue") boolean overdue,
                                    @Param("orderBy") String orderBy,
                                    @Param("today") LocalDate today,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM job_application " +
            "WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "<if test='keyword != null'>AND (company LIKE CONCAT('%',#{keyword},'%') OR `position` LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'>AND `status` = #{status} </if>" +
            "<if test='channel != null'>AND channel = #{channel} </if>" +
            "<if test='overdue'>AND follow_up_at IS NOT NULL AND follow_up_at &lt; #{today} AND `status` NOT IN ('offer','rejected','dropped','archived') </if>" +
            "</script>")
    long countPage(@Param("userId") Long userId,
                   @Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("channel") String channel,
                   @Param("overdue") boolean overdue,
                   @Param("today") LocalDate today);

    /** 编辑字段（不包含 status：状态只能走 updateStatus，保证必有 status_change 事件） */
    @Update("UPDATE job_application SET company = #{company}, `position` = #{position}, channel = #{channel}, location = #{location}, " +
            "job_url = #{jobUrl}, applied_at = #{appliedAt}, priority = #{priority}, salary_range = #{salaryRange}, " +
            "resume_version = #{resumeVersion}, contact = #{contact}, next_action = #{nextAction}, follow_up_at = #{followUpAt}, " +
            "notes = #{notes}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND u_id = #{uId} AND deleted_at IS NULL")
    int updateByIdAndUser(JobApplication application);

    @Update("UPDATE job_application SET `status` = #{status}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND u_id = #{userId} AND deleted_at IS NULL")
    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE job_application SET follow_up_at = #{followUpAt}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND u_id = #{userId} AND deleted_at IS NULL")
    int updateFollowUp(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("followUpAt") LocalDate followUpAt,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE job_application SET deleted_at = #{deletedAt}, updated_at = #{deletedAt} " +
            "WHERE id = #{id} AND u_id = #{userId} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id,
                   @Param("userId") Long userId,
                   @Param("deletedAt") LocalDateTime deletedAt);

    @Select("SELECT COUNT(*) FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL")
    long countTotal(@Param("userId") Long userId);

    /** 按状态分组统计：返回 [{code, cnt}] */
    @Select("SELECT `status` AS code, COUNT(*) AS cnt FROM job_application " +
            "WHERE u_id = #{userId} AND deleted_at IS NULL GROUP BY `status`")
    List<Map<String, Object>> countByStatus(@Param("userId") Long userId);

    /** 按渠道分组统计：返回 [{code, cnt}] */
    @Select("SELECT channel AS code, COUNT(*) AS cnt FROM job_application " +
            "WHERE u_id = #{userId} AND deleted_at IS NULL GROUP BY channel")
    List<Map<String, Object>> countByChannel(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "AND follow_up_at IS NOT NULL AND follow_up_at < #{today} " +
            "AND `status` NOT IN ('offer','rejected','dropped','archived')")
    long countOverdue(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Select("SELECT COUNT(*) FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "AND follow_up_at = #{today} " +
            "AND `status` NOT IN ('offer','rejected','dropped','archived')")
    long countDueToday(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** 逾期跟进列表（列表页置顶分组，按跟进日期升序） */
    @Select("SELECT * FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "AND follow_up_at IS NOT NULL AND follow_up_at < #{today} " +
            "AND `status` NOT IN ('offer','rejected','dropped','archived') " +
            "ORDER BY follow_up_at ASC, id DESC LIMIT #{limit}")
    List<JobApplication> selectOverdue(@Param("userId") Long userId,
                                       @Param("today") LocalDate today,
                                       @Param("limit") int limit);

    /** 今日跟进列表（列表页置顶分组） */
    @Select("SELECT * FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "AND follow_up_at = #{today} " +
            "AND `status` NOT IN ('offer','rejected','dropped','archived') " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<JobApplication> selectDueToday(@Param("userId") Long userId,
                                        @Param("today") LocalDate today,
                                        @Param("limit") int limit);

    /** 导出用：当前用户全部未删除记录（按投递日期倒序） */
    @Select("SELECT * FROM job_application WHERE u_id = #{userId} AND deleted_at IS NULL " +
            "ORDER BY applied_at DESC, id DESC")
    List<JobApplication> selectAllForExport(@Param("userId") Long userId);

    /** 回收站：当前用户已软删除的记录（按删除时间倒序，最多 200 条） */
    @Select("SELECT * FROM job_application WHERE u_id = #{userId} AND deleted_at IS NOT NULL " +
            "ORDER BY deleted_at DESC, id DESC LIMIT 200")
    List<JobApplication> selectDeleted(@Param("userId") Long userId);

    /** 恢复软删除记录 */
    @Update("UPDATE job_application SET deleted_at = NULL, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND u_id = #{userId} AND deleted_at IS NOT NULL")
    int restore(@Param("id") Long id,
                @Param("userId") Long userId,
                @Param("updatedAt") LocalDateTime updatedAt);
}