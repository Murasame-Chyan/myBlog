package com.murasame.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.AbstractMap;
import java.util.List;

@Mapper
public interface FollowMapper {

    @Insert("INSERT IGNORE INTO user_follows (follower_id, followee_id, created_at) VALUES (#{followerId}, #{followeeId}, NOW())")
    int insert(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Delete("DELETE FROM user_follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int delete(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int exists(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT followee_id FROM user_follows WHERE follower_id = #{followerId} ORDER BY created_at DESC")
    List<Long> findFollowingIds(@Param("followerId") Long followerId);

    @Select("SELECT follower_id FROM user_follows WHERE followee_id = #{followeeId} ORDER BY created_at DESC")
    List<Long> findFollowerIds(@Param("followeeId") Long followeeId);

    @Select("SELECT count(*) FROM user_follows WHERE follower_id = #{userId}")
    int countFollowing(@Param("userId") Long userId);

    @Select("SELECT count(*) FROM user_follows WHERE followee_id = #{userId}")
    int countFollowers(@Param("userId") Long userId);

    @Select("SELECT follower_id, followee_id FROM user_follows ORDER BY follower_id")
    List<AbstractMap.SimpleEntry<Long, Long>> findAllForWarmup();
}
