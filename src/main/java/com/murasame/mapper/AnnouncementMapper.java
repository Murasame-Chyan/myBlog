package com.murasame.mapper;

import com.murasame.entity.SysAnnouncement;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 公告 Mapper
 */
@Mapper
public interface AnnouncementMapper {

    /** 分页查询公告列表，可按状态筛选 */
    @Select("<script>" +
            "SELECT * FROM sys_announcement WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status} </if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<SysAnnouncement> findAll(@Param("status") Integer status,
                                  @Param("pageSize") int pageSize,
                                  @Param("offset") int offset);

    /** 统计数量（与 findAll 配套，筛选条件一致） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_announcement WHERE 1=1 " +
            "<if test='status != null'>AND status = #{status} </if>" +
            "</script>")
    long count(@Param("status") Integer status);

    /** 按 ID 查 */
    @Select("SELECT * FROM sys_announcement WHERE id = #{id}")
    SysAnnouncement findById(@Param("id") Long id);

    /** 新增公告 */
    @Insert("INSERT INTO sys_announcement (title, content, status, publisher_id) " +
            "VALUES (#{title}, #{content}, #{status}, #{publisherId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysAnnouncement announcement);

    /** 更新公告内容（不更新状态，状态由 publish/withdraw 单独控制） */
    @Update("UPDATE sys_announcement SET title = #{title}, content = #{content} " +
            "WHERE id = #{id}")
    int update(SysAnnouncement announcement);

    /** 更新状态（发布/撤回时用），发布时记录 publish_time */
    @Update("UPDATE sys_announcement SET status = #{status}, " +
            "publish_time = #{publishTime} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("publishTime") java.time.LocalDateTime publishTime);

    /** 删除公告 */
    @Delete("DELETE FROM sys_announcement WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
