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

	@Update("UPDATE users SET avatar = #{avatarUrl} WHERE id = #{userId}")
	int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

	@Select("SELECT * FROM users WHERE email=#{email}")
	Users getUserByEmail(@Param("email") String email);

	@Insert("INSERT INTO users (nickname, email, password) VALUES (#{nickname}, #{email}, #{password})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insertUser(Users user);

	@Update("UPDATE users SET nickname=#{nickname}, intro=#{intro}, email=#{email}, gender=#{gender}, github_username=#{githubUsername}, github_token=#{githubToken} WHERE id=#{id}")
	int updateUser(Users user);

	@Select("SELECT github_token FROM users WHERE id=#{id}")
	String getGithubTokenById(@Param("id") Long id);

	@Select("SELECT id FROM users WHERE github_username=#{githubUsername} AND github_token IS NOT NULL AND github_token != '' LIMIT 1")
	Long findUserIdWithTokenByGithubUsername(@Param("githubUsername") String githubUsername);

    @Update("UPDATE users SET password = #{password} WHERE email = #{email}")
    int updatePassword(@Param("email") String email, @Param("password") String password);

    @Update("UPDATE users SET follower_count = follower_count + 1 WHERE id = #{userId}")
    int incrementFollowerCount(@Param("userId") Long userId);

    @Update("UPDATE users SET follower_count = GREATEST(follower_count - 1, 0) WHERE id = #{userId}")
    int decrementFollowerCount(@Param("userId") Long userId);

    @Update("UPDATE users SET following_count = following_count + 1 WHERE id = #{userId}")
    int incrementFollowingCount(@Param("userId") Long userId);

    @Update("UPDATE users SET following_count = GREATEST(following_count - 1, 0) WHERE id = #{userId}")
    int decrementFollowingCount(@Param("userId") Long userId);

    // 原子更新经验值，同时更新等级；上限 999999
    @Update("UPDATE users SET exp = LEAST(exp + #{delta}, 999999), level = #{newLevel} WHERE id = #{userId}")
    int addExp(@Param("userId") Long userId, @Param("delta") int delta, @Param("newLevel") int newLevel);

    @Select("SELECT exp FROM users WHERE id = #{userId}")
    int getExpById(@Param("userId") Long userId);

    @Update("UPDATE users SET achievement = #{achievement} WHERE id = #{userId}")
    int updateAchievement(@Param("userId") Long userId, @Param("achievement") String achievement);

    @Select("SELECT achievement FROM users WHERE id = #{userId}")
    String getAchievementById(@Param("userId") Long userId);
}
