package com.murasame.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserLikeMapper {

    @Insert("INSERT IGNORE INTO user_likes (user_id, blog_id) VALUES (#{userId}, #{blogId})")
    int insert(@Param("userId") Long userId, @Param("blogId") Long blogId);

    @Delete("DELETE FROM user_likes WHERE user_id = #{userId} AND blog_id = #{blogId}")
    int delete(@Param("userId") Long userId, @Param("blogId") Long blogId);

    @Select("SELECT COUNT(*) FROM user_likes WHERE user_id = #{userId} AND blog_id = #{blogId}")
    int exists(@Param("userId") Long userId, @Param("blogId") Long blogId);

    @Select("SELECT blog_id FROM user_likes WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Long> findBlogIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id, blog_id FROM user_likes ORDER BY user_id")
    List<java.util.AbstractMap.SimpleEntry<Long, Long>> findAllForWarmup();
}
