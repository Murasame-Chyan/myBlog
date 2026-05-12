package com.murasame.service;

import com.murasame.entity.Users;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
	Users getUserById(Long id);

	String getNicknameById(Long id);

	int updateLikedBId(Long userId, String likedBId);

	boolean isBlogLiked(Long userId, Long blogId);

	int updateAvatar(Long userId, String avatarUrl);

	Users register(String email, String nickname, String password);

	Users login(String email, String password);
}