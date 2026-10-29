package com.murasame.mapper;

import com.murasame.entity.JobApplicationEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 求职投递时间线事件 Mapper。事件只追加，不提供 update/delete。
 */
@Mapper
public interface JobApplicationEventMapper {

    @Insert("INSERT INTO job_application_event (application_id, u_id, `type`, occurred_at, from_status, to_status, title, content, meta, created_at) " +
            "VALUES (#{applicationId}, #{uId}, #{type}, #{occurredAt}, #{fromStatus}, #{toStatus}, #{title}, #{content}, #{meta}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobApplicationEvent event);

    @Select("SELECT * FROM job_application_event WHERE application_id = #{applicationId} AND u_id = #{userId} " +
            "ORDER BY occurred_at DESC, id DESC")
    List<JobApplicationEvent> selectByApplication(@Param("applicationId") Long applicationId,
                                                  @Param("userId") Long userId);

    /** 最近一次状态变更事件（用于计算"当前状态已停留天数"） */
    @Select("SELECT * FROM job_application_event WHERE application_id = #{applicationId} AND u_id = #{userId} " +
            "AND `type` = 'status_change' ORDER BY occurred_at DESC, id DESC LIMIT 1")
    JobApplicationEvent selectLatestStatusChange(@Param("applicationId") Long applicationId,
                                                  @Param("userId") Long userId);
}
