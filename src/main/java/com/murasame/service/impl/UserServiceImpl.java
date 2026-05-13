package com.murasame.service.impl;

import com.murasame.entity.Users;
import com.murasame.mapper.UserMapper;
import com.murasame.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
	@Resource
	private UserMapper userMapper;

	@Resource
	private BCryptPasswordEncoder passwordEncoder;

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
		userMapper.updateUser(user);
		return userMapper.getUserById(user.getId());
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
}
