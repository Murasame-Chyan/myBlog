package com.murasame.mapper;

import com.murasame.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /** 管理端：用户分页列表，可按昵称/邮箱搜索 */
    @Select("<script>" +
            "SELECT * FROM users WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (nickname LIKE CONCAT('%',#{keyword},'%') OR email LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "ORDER BY id ASC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<Users> findAllPage(@Param("keyword") String keyword,
                            @Param("pageSize") int pageSize,
                            @Param("offset") int offset);

    /** 管理端：用户总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM users WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (nickname LIKE CONCAT('%',#{keyword},'%') OR email LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "</script>")
    long countAll(@Param("keyword") String keyword);

    /** 更新用户状态（封号/解封） */
    @Update("UPDATE users SET status = #{status} WHERE id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /** 查所有用户 ID（公告等群发通知用） */
    @Select("SELECT id FROM users")
    List<Long> findAllIds();

    /**
     * 管理端编辑用户：只更新昵称、简介、邮箱三个字段。
     * 不触碰 github_token，避免把库中已加密的 token 二次加密。
     */
    @Update("UPDATE users SET nickname = #{nickname}, intro = #{intro}, email = #{email} WHERE id = #{id}")
    int updateAdminFields(@Param("id") Long id,
                          @Param("nickname") String nickname,
                          @Param("intro") String intro,
                          @Param("email") String email);
}
