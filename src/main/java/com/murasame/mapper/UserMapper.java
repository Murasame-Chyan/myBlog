package com.murasame.mapper;

import com.murasame.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
	@Select("SELECT * FROM users WHERE id=#{id}")
	Users getUserById(@Param("id") Long id);

	@Select("SELECT nickname FROM users WHERE id=#{id}")
	String getNicknameById(@Param("id") Long id);

	@Update("UPDATE users SET liked_b_id = #{likedBId} WHERE id = #{userId}")
	int updateLikedBId(@Param("userId") Long userId, @Param("likedBId") String likedBId);

	@Update("UPDATE users SET avatar = #{avatarUrl} WHERE id = #{userId}")
	int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

	@Select("SELECT * FROM users WHERE email=#{email}")
	Users getUserByEmail(@Param("email") String email);

	@Insert("INSERT INTO users (nickname, email, password) VALUES (#{nickname}, #{email}, #{password})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertUser(Users user);
}
