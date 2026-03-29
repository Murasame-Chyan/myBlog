package com.murasame.mapper;

import com.murasame.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
	@Select("SELECT * FROM users WHERE id=#{id}")
	Users getUserById(@Param("id") Long id);

	@Select("SELECT nickname FROM users WHERE id=#{id}")
	String getNicknameById(@Param("id") Long id);

	@Update("UPDATE users SET liked_b_id = #{likedBId} WHERE id = #{userId}")
	int updateLikedBId(@Param("userId") Long userId, @Param("likedBId") String likedBId);
}
