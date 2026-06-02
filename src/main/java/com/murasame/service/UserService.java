package com.murasame.service;

import com.murasame.entity.Users;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
	Users getUserById(Long id);

	String getNicknameById(Long id);

	int updateAvatar(Long userId, String avatarUrl);

	Users register(String email, String nickname, String password);

	Users login(String email, String password);

	Users getUserByEmail(String email);

	Users updateProfile(Users user);

	int calculateLevel(int exp);

    void resetPassword(String email, String newPassword);

    /** 获取用户解密后的 GitHub Token，用于个人 API 调用；无 token 时返回 null */
    String getDecryptedGithubToken(Long userId);

    /** 根据 GitHub 用户名查找已绑定 token 的用户，取第一个匹配者的解密 token */
    String findGithubTokenByGithubUsername(String githubUsername);

    /** 原子增加经验值并更新等级，返回新经验值 */
    int addExp(Long userId, int delta);

    /** 从 DB 读取当前经验值 */
    int getExp(Long userId);

    /** 当前等级起始经验值 */
    int getCurrentLevelExp(int exp);

    /** 下一等级所需总经验，已满级返回 -1 */
    int getExpForNextLevel(int exp);
}