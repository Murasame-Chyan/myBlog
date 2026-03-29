package com.murasame.service;

import com.murasame.entity.Users;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
	Users getUserById(Long id);

	String getNicknameById(Long id);

	int updateLikedBId(Long userId, String likedBId);

	boolean isBlogLiked(Long userId, Long blogId);
}