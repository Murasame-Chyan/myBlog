package com.murasame.mapper;

import com.murasame.entity.SysNotification;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper {

    /** 插入通知 */
    @Insert("INSERT INTO sys_notification (user_id, title, content, type) " +
            "VALUES (#{userId}, #{title}, #{content}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysNotification notification);

    /** 查某用户的通知列表（按时间倒序） */
    @Select("SELECT * FROM sys_notification WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<SysNotification> findByUserId(@Param("userId") Long userId);

    /** 查某用户未读通知数（顶部小铃铛徽标用） */
    @Select("SELECT COUNT(*) FROM sys_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    /** 标记单条通知为已读 */
    @Update("UPDATE sys_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 标记某用户全部通知为已读 */
    @Update("UPDATE sys_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);

    /** 删除通知 */
    @Delete("DELETE FROM sys_notification WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}
