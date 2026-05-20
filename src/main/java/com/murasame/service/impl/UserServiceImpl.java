package com.murasame.service.impl;

import com.murasame.entity.Users;
import com.murasame.mapper.UserMapper;
import com.murasame.service.UserService;
import com.murasame.util.AesEncryptionUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	@Resource
	private UserMapper userMapper;

	@Resource
	private BCryptPasswordEncoder passwordEncoder;

	@Resource
	private AesEncryptionUtil aesEncryptionUtil;

	@Override
	public Users getUserById(Long id) {
		return userMapper.getUserById(id);
	}

	@Override
	public String getNicknameById(Long id) {
		return userMapper.getNicknameById(id);
	}

	@Override
	public int updateAvatar(Long userId, String avatarUrl) {
		return userMapper.updateAvatar(userId, avatarUrl);
	}

	@Override
	public Users register(String email, String nickname, String password) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("邮箱不能为空");
		}
		if (password == null || password.length() < 6) {
			throw new IllegalArgumentException("密码至少6位");
		}
		Users existing = userMapper.getUserByEmail(email);
		if (existing != null) {
			throw new IllegalArgumentException("该邮箱已被注册");
		}
		Users user = new Users();
		// 未提供昵称时，默认使用邮箱前缀作为昵称
		user.setNickname(nickname != null && !nickname.isBlank() ? nickname : email.split("@")[0]);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		userMapper.insertUser(user);
		return user;
	}

	@Override
	public Users updateProfile(Users user) {
		if (user.getNickname() == null || user.getNickname().isBlank()) {
			throw new IllegalArgumentException("昵称不能为空");
		}
		// 校验邮箱不被其他用户占用
		if (user.getEmail() != null && !user.getEmail().isBlank()) {
			Users existing = userMapper.getUserByEmail(user.getEmail());
			if (existing != null && !existing.getId().equals(user.getId())) {
				throw new IllegalArgumentException("该邮箱已被其他用户使用");
			}
		}
		// GitHub Token 加密后存储
		if (user.getGithubToken() != null && !user.getGithubToken().isBlank()) {
			try {
				user.setGithubToken(aesEncryptionUtil.encrypt(user.getGithubToken()));
			} catch (Exception e) {
				log.error("Failed to encrypt GitHub token for user {}", user.getId(), e);
				throw new IllegalArgumentException("GitHub Token 加密失败");
			}
		}
		userMapper.updateUser(user);
		return userMapper.getUserById(user.getId());
	}

	@Override
	public String getDecryptedGithubToken(Long userId) {
		String encrypted = userMapper.getGithubTokenById(userId);
		if (encrypted == null || encrypted.isBlank()) {
			return null;
		}
		try {
			return aesEncryptionUtil.decrypt(encrypted);
		} catch (Exception e) {
			log.error("Failed to decrypt GitHub token for user {}", userId, e);
			return null;
		}
	}

	@Override
	public String findGithubTokenByGithubUsername(String githubUsername) {
		Long userId = userMapper.findUserIdWithTokenByGithubUsername(githubUsername);
		if (userId == null) {
			return null;
		}
		return getDecryptedGithubToken(userId);
	}

	@Override
	public Users login(String email, String password) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return null;
		}
		Users user = userMapper.getUserByEmail(email);
		if (user == null || user.getPassword() == null) {
			return null;
		}
		if (!passwordEncoder.matches(password, user.getPassword())) {
			return null;
		}
		return user;
	}

	@Override
	public Users getUserByEmail(String email) {
		return userMapper.getUserByEmail(email);
	}

	// 经验值等级体系：共10级，阈值大致每级翻倍，从LV1(0)到LV10(600,000)
	private static final long[] LEVEL_THRESHOLDS = {
		0, 200, 1500, 4500, 10800, 28800, 65000, 140000, 300000, 600000
	};

	@Override
	public int calculateLevel(int exp) {
		int level = 1;
		for (int i = 1; i < LEVEL_THRESHOLDS.length; i++) {
			if (exp >= LEVEL_THRESHOLDS[i]) {
				level = i + 1;
			} else {
				break;
			}
		}
		return level;
	}

	@Override
	public void resetPassword(String email, String newPassword) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("邮箱不能为空");
		}
		if (newPassword == null || newPassword.length() < 6) {
			throw new IllegalArgumentException("密码至少6位");
		}
		Users user = userMapper.getUserByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("该邮箱未注册");
		}
		userMapper.updatePassword(email, passwordEncoder.encode(newPassword));
	}
}
