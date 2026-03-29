package com.murasame.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murasame.entity.Users;
import com.murasame.mapper.UserMapper;
import com.murasame.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
	@Resource
	private UserMapper userMapper;

	@Resource
	private ObjectMapper objectMapper;

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
}