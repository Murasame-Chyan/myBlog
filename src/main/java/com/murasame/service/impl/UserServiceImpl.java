package com.murasame.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murasame.entity.Users;
import com.murasame.mapper.UserMapper;
import com.murasame.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
	@Resource
	private UserMapper userMapper;

	@Resource
	private ObjectMapper objectMapper;

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
	public int updateLikedBId(Long userId, String likedBId) {
		return userMapper.updateLikedBId(userId, likedBId);
	}

	@Override
	public boolean isBlogLiked(Long userId, Long blogId) {
		if (userId == null) {
			return false;
		}
		Users user = userMapper.getUserById(userId);
		if (user == null || user.getLiked_b_id() == null || user.getLiked_b_id().isEmpty()) {
			return false;
		}
		try {
			List<Long> likedBlogIds = objectMapper.readValue(user.getLiked_b_id(), new TypeReference<>() {
			});
			return likedBlogIds.contains(blogId);
		} catch (Exception e) {
			return false;
		}
	}

	public List<Long> getLikedBlogIds(Long userId) {
		if (userId == null) {
			return new ArrayList<>();
		}
		Users user = userMapper.getUserById(userId);
		if (user == null || user.getLiked_b_id() == null || user.getLiked_b_id().isEmpty()) {
			return new ArrayList<>();
		}
		try {
			return objectMapper.readValue(user.getLiked_b_id(), new TypeReference<>() {
			});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	public boolean addBlogToLiked(Long userId, Long blogId) {
		if (userId == null) {
			return false;
		}
		List<Long> likedBlogIds = getLikedBlogIds(userId);
		if (likedBlogIds.contains(blogId)) {
			return false;
		}
		likedBlogIds.add(blogId);
		try {
			String json = objectMapper.writeValueAsString(likedBlogIds);
			return updateLikedBId(userId, json) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean removeBlogFromLiked(Long userId, Long blogId) {
		if (userId == null) {
			return false;
		}
		List<Long> likedBlogIds = getLikedBlogIds(userId);
		if (!likedBlogIds.contains(blogId)) {
			return false;
		}
		likedBlogIds.remove(blogId);
		try {
			String json = objectMapper.writeValueAsString(likedBlogIds);
			return updateLikedBId(userId, json) > 0;
		} catch (Exception e) {
			return false;
		}
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
}